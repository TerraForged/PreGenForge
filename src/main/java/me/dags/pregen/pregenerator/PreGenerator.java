package me.dags.pregen.pregenerator;

import me.dags.pregen.PreGenForge;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class PreGenerator {

    public static int bufferSize = 40;

    private final int regionCount;
    private final int chunkCount;
    private final PreGenConfig config;
    private final WorldServer worldServer;
    private final Iterator<PreGenRegion> regions;
    private final List<Chunk> chunks = new ArrayList<>(bufferSize * 2);

    private boolean started = false;
    private long ticks = 0;
    private long region = 0;
    private long visitCount = 0;
    private long completeCount = 0;
    private PreGenRegion.ChunkIterator chunkIterator = null;

    public PreGenerator(WorldServer worldServer, PreGenConfig config) {
        List<PreGenRegion> regions = config.getRegions();
        this.config = config;
        this.worldServer = worldServer;
        this.regions = regions.iterator();
        this.regionCount = regions.size();
        this.chunkCount = (PreGenRegion.SIZE * PreGenRegion.SIZE * regionCount) - (config.getChunkIndex() + 1);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        ticks++;

        if (feedback()) {
            printStats();
        }

        if (cleanup()) {
            PreGenForge.savePreGenerator(worldServer, config);
            System.gc();
            worldServer.getChunkProvider().flushToDisk();
        }

        if (isComplete()) {
            drainQueue();
            printDone();
            cancel();
            return;
        }

        if (chunks.size() > bufferSize / 2) {
            drainQueue();
            return;
        }

        visitRegion();
    }

    public void start() {
        printStarted();
        started = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void cancel() {
        if (pause()) {
            PreGenForge.deletePreGenerator(worldServer);
            PreGenForge.print("Cancelled pre-generator on world: " + worldServer.getWorldInfo().getWorldName());
        }
    }

    public boolean pause() {
        if (started) {
            started = false;
            PreGenForge.savePreGenerator(worldServer, config);
            MinecraftForge.EVENT_BUS.unregister(this);
            PreGenForge.print("Paused pre-generator on world: " + worldServer.getWorldInfo().getWorldName());
            return true;
        }
        return false;
    }

    private boolean isComplete() {
        return !regions.hasNext() && !chunkIterator.hasNext();
    }

    private boolean feedback() {
        return ticks % 200 == 0;
    }

    private boolean cleanup() {
        return ticks % 600 == 0;
    }

    private void drainQueue() {
        Iterator<Chunk> iterator = chunks.iterator();
        while (iterator.hasNext()) {
            Chunk next = iterator.next();
            if (next.isLoaded()) {
                iterator.remove();
                completeCount++;
                worldServer.getChunkProvider().queueUnload(next);
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
            this.region++;
        }

        int limit = bufferSize / 2;
        while (chunkIterator.hasNext() && limit-- > 0) {
            ChunkPos pos = chunkIterator.next();
            config.setChunkIndex(chunkIterator.index());

            Chunk chunk = worldServer.getChunkProvider().getChunk(pos.x, pos.z, true, true);
            if (chunk != null) {
                visitCount++;
                chunks.add(chunk);
            }
        }
    }

    private void printStarted() {
        PreGenForge.print("Started pre-generating chunks for world: " + worldServer.getWorldInfo().getWorldName());
    }

    private void printStats() {
        PreGenForge.print(
                "==================================",
                String.format("Progress: %.2f%%", (completeCount * 100F) / chunkCount),
                String.format("Regions: %s / %s", region, regionCount),
                String.format("Chunks: %s / %s", completeCount, chunkCount)
        );
    }

    private void printDone() {
        PreGenForge.print(
                "==================================",
                String.format("Completed chunks: %s", chunkCount),
                String.format("Time taken: %s secs", (ticks / 20))
        );
    }
}
