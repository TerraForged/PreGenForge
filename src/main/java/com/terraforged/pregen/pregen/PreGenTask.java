package com.terraforged.pregen.pregen;

import com.google.common.base.Stopwatch;
import com.terraforged.pregen.IO;
import com.terraforged.pregen.Log;
import com.terraforged.pregen.PreGen;
import com.terraforged.pregen.task.Task;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreGenTask implements Task {

    private static final long STATS_INTERVAL = 30;
    private static final long CLEANUP_INTERVAL = 60 * 3;

    private final int chunkCount;
    private final String name;
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

    public PreGenTask(ServerWorld world, PreGenConfig config) {
        List<PreGenRegion> regions = config.getRegions();
        this.config = config;
        this.world = world;
        this.name = getName(world);
        this.regions = regions.iterator();
        this.chunkCount = (PreGenRegion.SIZE * PreGenRegion.SIZE * regions.size()) - (config.getChunkIndex() + 1);
    }

    public String getName() {
        return name;
    }

    public boolean start() {
        if (stopped) {
            stopped = false;
            statsTimer.reset().start();
            cleanupTimer.reset().start();
            PreGen.getInstance().getScheduler().submit(this);
            Log.printf("(%s) Started", name);
            return true;
        }
        return false;
    }

    public boolean pause() {
        if (!stopped) {
            stopped = true;
            statsTimer.stop().reset();
            cleanupTimer.stop().reset();
            IO.saveConfig(config, world);
            Log.printf("(%s) Paused", name);
            return true;
        }
        return false;
    }

    public boolean cancel() {
        stopped = true;
        if (IO.deleteConfig(world)) {
            Log.printf("(%s) Disposed", name);
        }
        return true;
    }

    @Override
    public boolean isComplete() {
        if (stopped) {
            return true;
        }
        if (chunkIterator == null || !chunkIterator.hasNext()) {
            if (!regions.hasNext()) {
                return true;
            }
            PreGenRegion region = regions.next();
            // record the next region index so we can start there after a restart
            config.setRegionIndex(config.getRegionIndex() + 1);

            int chunkIndex = chunkIterator == null ? config.getChunkIndex() : -1;
            chunkIterator = region.iterator(chunkIndex);
        }
        return !chunkIterator.hasNext();
    }

    @Override
    public boolean perform() {
        // print progress stats every X seconds
        if (shouldPrintStats()) {
            printStats();
            IO.saveConfig(config, world);
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

        if (!isComplete()) {
            ChunkPos pos = chunkIterator.next();
            // record the next chunk index so we can start there after a restart
            config.setChunkIndex(chunkIterator.index() + 1);

            // bool flag must be true as this tells the chunk provider to generate the chunk if it doesn't exist
            world.getChunkProvider().getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
            chunks++;
            if (isComplete()) {
                Log.printf("(%s) Complete!", name);
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
        Log.printf("(%s) Progress: %.2f%%, Chunks: %s/%s (%.2f/sec), ETA: %s", name, prog, chunks, chunkCount, rate, eta);
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

    public static String getName(ServerWorld world) {
        String name = world.getWorldInfo().getWorldName();
        ResourceLocation dim = world.getDimension().getType().getRegistryName();
        if (dim == null) {
            return name;
        }
        return name + ":" + dim.getPath();
    }
}
