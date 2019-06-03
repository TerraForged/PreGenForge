package me.dags.pregen.pregenerator;

import com.google.common.base.Stopwatch;
import me.dags.pregen.PreGenForge;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PreGenerator {

    private static final long STATS_INTERVAL = 30;
    private static final long CLEANUP_INTERVAL = 60 * 3;

    private final int chunkCount;
    private final PreGenConfig config;
    private final WorldServer worldServer;
    private final Iterator<PreGenRegion> regions;
    private final List<Chunk> buffer;

    private final Stopwatch statsTimer = Stopwatch.createUnstarted();
    private final Stopwatch cleanupTimer = Stopwatch.createUnstarted();

    private long chunks = 0;
    private long prevChunks = 0;
    private boolean running = false;
    private PreGenRegion.ChunkIterator chunkIterator = null;

    public PreGenerator(WorldServer worldServer, PreGenConfig config) {
        List<PreGenRegion> regions = config.getRegions();
        this.config = config;
        this.worldServer = worldServer;
        this.regions = regions.iterator();
        this.buffer = new ArrayList<>(config.getLimit());
        int regionCount = regions.size();
        this.chunkCount = (PreGenRegion.SIZE * PreGenRegion.SIZE * regionCount) - (config.getChunkIndex() + 1);
    }

    public String getName() {
        return worldServer.getWorldInfo().getWorldName();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (statsTimer.elapsed(TimeUnit.SECONDS) >= STATS_INTERVAL) {
            printStats();
            PreGenForge.savePreGenerator(worldServer, config);
            statsTimer.reset().start();
        }

        if (cleanupTimer.elapsed(TimeUnit.SECONDS) >= CLEANUP_INTERVAL) {
            statsTimer.stop();
            worldServer.getChunkProvider().flushToDisk();
            System.gc();
            cleanupTimer.reset().start();
            statsTimer.start();
        }

        if (isComplete()) {
            drainQueue();
            printDone();
            cancel();
            return;
        }

        if (buffer.size() > config.getLimit()) {
            drainQueue();
            return;
        }

        visitRegion();
    }

    public void start() {
        running = true;
        statsTimer.start();
        cleanupTimer.start();
        MinecraftForge.EVENT_BUS.register(this);
        PreGenForge.printf("Started for world: %s", getName());
    }

    public void cancel() {
        pause();
        if (PreGenForge.deletePreGenerator(worldServer)) {
            PreGenForge.printf("Removed for world: %s", getName());
        }
    }

    public boolean pause() {
        if (running) {
            running = false;
            statsTimer.stop().reset();
            cleanupTimer.stop().reset();
            PreGenForge.savePreGenerator(worldServer, config);
            MinecraftForge.EVENT_BUS.unregister(this);
            PreGenForge.printf("Paused for world: %s", getName());
            return true;
        }
        return false;
    }

    public void setLimit(int limit) {
        config.setLimit(limit);
        PreGenForge.printf("Set limit: %s", limit);
    }

    private boolean isComplete() {
        return !regions.hasNext() && !chunkIterator.hasNext();
    }

    private void drainQueue() {
        Iterator<Chunk> iterator = buffer.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            if (chunk.isLoaded()) {
                iterator.remove();
                chunks++;
                worldServer.getChunkProvider().queueUnload(chunk);
            }
        }
    }

    private void visitRegion() {
        if (chunkIterator == null || !chunkIterator.hasNext()) {
            if (!regions.hasNext()) {
                return;
            }

            PreGenRegion region = regions.next();
            config.setRegionIndex(config.getRegionIndex() + 1);

            int chunkIndex = chunkIterator == null ? config.getChunkIndex() : -1;
            chunkIterator = region.iterator(chunkIndex);
        }

        int limit = config.getLimit() / 2;
        while (chunkIterator.hasNext() && limit-- > 0) {
            ChunkPos pos = chunkIterator.next();
            config.setChunkIndex(chunkIterator.index());

            Chunk chunk = worldServer.getChunkProvider().getChunk(pos.x, pos.z, true, true);
            if (chunk != null) {
                buffer.add(chunk);
            }
        }
    }

    private void printStats() {
        float prog = getProgress();
        float rate = getRate();
        PreGenForge.printf("Progress: %.2f%%, Chunks: %s/%s (%.2f/sec)", prog, chunks, chunkCount, rate);
    }

    private void printDone() {
        PreGenForge.print("Complete!");
    }

    private float getProgress() {
        return (chunks * 100F) / chunkCount;
    }

    private float getRate() {
        float chunkDelta = chunks - prevChunks;
        float timeDelta = statsTimer.elapsed(TimeUnit.MILLISECONDS) / 1000F;
        prevChunks = chunks;
        return chunkDelta / timeDelta;
    }
}
