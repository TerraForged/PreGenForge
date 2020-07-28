package com.terraforged.pregen.pregen;

import com.google.common.base.Stopwatch;
import com.terraforged.pregen.IO;
import com.terraforged.pregen.Log;
import com.terraforged.pregen.PreGen;
import com.terraforged.pregen.task.Task;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreGenTask implements Task {

    protected static final long STATS_INTERVAL = 30;
    protected static final long CLEANUP_INTERVAL = 60 * 3;

    protected final int chunkCount;
    protected final String name;
    protected final ServerWorld world;
    protected final PreGenConfig config;
    protected final Iterator<PreGenRegion> regions;
    protected final LinkedList<ChunkPos> queue = new LinkedList<>();

    protected final Stopwatch statsTimer = Stopwatch.createUnstarted();
    protected final Stopwatch cleanupTimer = Stopwatch.createUnstarted();

    protected long chunks = 0;
    protected long prevChunks = 0;
    protected int prevProgress = 0;
    protected boolean stopped = true;

    protected long rateCount = 0L;
    protected double totalRate = 0D;

    protected PreGenRegion.ChunkIterator chunkIterator = null;

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
                return queue.isEmpty();
            }

            // chunkIterator is null on first run of the task, in which case get the index from the config
            // so that generation can continue after restarts, otherwise use the default index
            int chunkIndex = chunkIterator == null ? config.getChunkIndex() : PreGenRegion.DEFAULT_INDEX;

            while (regions.hasNext()) {
                PreGenRegion region = regions.next();
                // record the next region index so we can start there after a restart
                config.setRegionIndex(config.getRegionIndex() + 1);

                chunkIterator = region.iterator(chunkIndex);

                // false if the config was set at max-index
                if (chunkIterator.hasNext()) {
                    return false;
                }

                // try the next region at the start
                chunkIndex = PreGenRegion.DEFAULT_INDEX;
            }
        }
        // has finished generating if no more chunks & the work queue is empty
        return !chunkIterator.hasNext() && queue.isEmpty();
    }

    @Override
    public boolean perform() {
        PreGenTask.drive(world.getChunkProvider(), 50);

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

        if (!queue.isEmpty()) {
            clearQueue();
            if (isComplete()) {
                Log.printf("(%s) Complete!", name);
                cancel();
                return false;
            }
            if (queue.size() > 50) {
                return true;
            }
        }

        while (chunkIterator.hasNext()) {
            ChunkPos pos = chunkIterator.next();
            // record the next chunk index so we can start there after a restart
            config.setChunkIndex(chunkIterator.index() + 1);

            // bool flag must be true as this tells the chunk provider to generate the chunk if it doesn't exist
            world.getChunkProvider().forceChunk(pos, true);
            queue.add(pos);
        }
        return true;
    }

    private void clearQueue() {
        Iterator<ChunkPos> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            if (world.getChunkProvider().isChunkLoaded(pos)) {
                chunks++;
                iterator.remove();
                world.forceChunk(pos.x, pos.z, false);
            }
        }
    }

    protected void cleanUp() {
        // force save the world, flushing enabled (suppressLog=false, flush=true, forced=true)
        world.getServer().save(false, true, true);
        // probably not necessary but run gc sweep
        System.gc();
        // restart timer
        cleanupTimer.reset().start();
    }

    protected boolean shouldPrintStats() {
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

    protected void printStats() {
        float prog = getProgress();
        float rate = getRate();
        double average = getAverageRate(rate);
        String eta = getETA(average);
        Log.printf("(%s) Progress: %.2f%%, Chunks: %s/%s (%.2f/sec), ETA: %s", name, prog, chunks, chunkCount, rate, eta);
    }

    private int getProgressStep(int steps) {
        return (((int) getProgress()) / steps) * steps;
    }

    private float getProgress() {
        return (chunks * 100F) / chunkCount;
    }

    private double getAverageRate(float rate) {
        totalRate += rate;
        rateCount++;
        return totalRate / rateCount;
    }

    private float getRate() {
        float chunkDelta = chunks - prevChunks;
        float timeDelta = statsTimer.elapsed(TimeUnit.MILLISECONDS) / 1000F;
        prevChunks = chunks;
        return chunkDelta / timeDelta;
    }

    private String getETA(double rate) {
        long time = Math.round((chunkCount - chunks) / rate);
        long hrs = time / 3600;
        long mins = (time - (hrs * 3600)) / 60;
        long secs = time - (hrs * 3600) - (mins * 60);
        return String.format("%sh:%sm:%ss", hrs, mins, secs);
    }

    protected static void drive(ServerChunkProvider provider, int count) {
        while (count-- > 0) {
            try {
                if (!provider.driveOneTask()) {
                    return;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
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
