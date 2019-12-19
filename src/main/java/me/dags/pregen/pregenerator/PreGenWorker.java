package me.dags.pregen.pregenerator;

import com.google.common.base.Stopwatch;
import me.dags.pregen.PreGenForge;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreGenWorker implements WorldWorkerManager.IWorker {

    private static final long STATS_INTERVAL = 30;
    private static final long CLEANUP_INTERVAL = 60 * 3;

    private final int chunkCount;
    private final ServerWorld world;
    private final PreGenConfig config;
    private final Iterator<PreGenRegion> regions;

    private final Stopwatch statsTimer = Stopwatch.createUnstarted();
    private final Stopwatch cleanupTimer = Stopwatch.createUnstarted();

    private long chunks = 0;
    private long prevChunks = 0;
    private int prevProgress = 0;
    private boolean stopped = true;
    private PreGenRegion.ChunkIterator chunkIterator = null;

    public PreGenWorker(ServerWorld world, PreGenConfig config) {
        List<PreGenRegion> regions = config.getRegions();
        this.config = config;
        this.world = world;
        this.regions = regions.iterator();
        this.chunkCount = (PreGenRegion.SIZE * PreGenRegion.SIZE * regions.size()) - (config.getChunkIndex() + 1);
    }

    public String getName() {
        return world.getWorldInfo().getWorldName();
    }

    public boolean start() {
        if (stopped) {
            stopped = false;
            statsTimer.reset().start();
            cleanupTimer.reset().start();
            WorldWorkerManager.addWorker(this);
            PreGenForge.printf("Started in world: %s", getName());
            return true;
        }
        return false;
    }

    public boolean pause() {
        if (!stopped) {
            stopped = true;
            statsTimer.stop().reset();
            cleanupTimer.stop().reset();
            PreGenForge.savePreGenerator(world, config);
            PreGenForge.printf("Paused for world: %s", getName());
            return true;
        }
        return false;
    }

    public boolean cancel() {
        stopped = true;
        if (PreGenForge.deletePreGenerator(world)) {
            PreGenForge.printf("Removed for world: %s", getName());
        }
        return true;
    }

    @Override
    public boolean hasWork() {
        if (stopped) {
            return false;
        }
        if (chunkIterator == null || !chunkIterator.hasNext()) {
            if (!regions.hasNext()) {
                return false;
            }
            PreGenRegion region = regions.next();
            config.setRegionIndex(config.getRegionIndex() + 1);

            int chunkIndex = chunkIterator == null ? config.getChunkIndex() : -1;
            chunkIterator = region.iterator(chunkIndex);
        }
        return chunkIterator.hasNext();
    }

    @Override
    public boolean doWork() {
        // print progress stats every X seconds
        if (shouldPrintStats()) {
            printStats();
            PreGenForge.savePreGenerator(world, config);
            statsTimer.reset().start();
        }

        // save world chunks to disk every X seconds, should also free up memory
        if (cleanupTimer.elapsed(TimeUnit.SECONDS) >= CLEANUP_INTERVAL) {
            // pause stats timer while cleanup is running
            statsTimer.stop();
            cleanUp();
            cleanupTimer.reset().start();
            statsTimer.start();
        }

        if (hasWork()) {
            ChunkPos pos = chunkIterator.next();
            world.getChunkProvider().getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
            chunks++;
            if (!hasWork()) {
                PreGenForge.print("Complete!");
                cancel();
                return false;
            }
            return true;
        }

        return false;
    }

    private void cleanUp() {
        // force save the world, flushing enabled (suppressLog=false, flush=true, forced=true)
        world.getServer().save(false, true, true);
        // probably not necessary but run gc sweep
        System.gc();
        // restart timer
        cleanupTimer.reset().start();
    }

    private boolean shouldPrintStats() {
        int progress = getProgressStep(10);
        if (progress - prevProgress > 10) {
            prevProgress = progress;
            return true;
        }

        if (statsTimer.elapsed(TimeUnit.SECONDS) >= STATS_INTERVAL) {
            prevProgress = progress;
            return true;
        }

        return false;
    }

    private void printStats() {
        float prog = getProgress();
        float rate = getRate();
        String eta = getETA(rate);
        PreGenForge.printf("Progress: %.2f%%, Chunks: %s/%s (%.2f/sec), ETA: %s", prog, chunks, chunkCount, rate, eta);
    }

    private int getProgressStep(int steps) {
        return (((int) getProgress()) / steps) * steps;
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

    private String getETA(float rate) {
        long time = Math.round((chunkCount - chunks) / rate);
        long hrs = time / 3600;
        long mins = (time - (hrs * 3600)) / 60;
        long secs = time - (hrs * 3600) - (mins * 60);
        return String.format("%sh:%sm:%ss", hrs, mins, secs);
    }
}
