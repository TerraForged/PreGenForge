package com.terraforged.pregen.task;

import com.google.common.base.Stopwatch;
import com.terraforged.pregen.util.IO;
import com.terraforged.pregen.util.Log;
import com.terraforged.pregen.PreGen;
import com.terraforged.pregen.pregen.PreGenConfig;
import com.terraforged.pregen.pregen.PreGenRegion;
import com.terraforged.pregen.util.TaskTimer;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTask implements Task {

    protected static final long STATS_INTERVAL = 30;
    protected static final long CLEANUP_INTERVAL = 60 * 2;

    protected final int chunkCount;
    protected final String name;
    protected final PreGenConfig config;
    protected final Iterator<PreGenRegion> regions;

    protected final TaskTimer taskTimer = new TaskTimer();
    protected final Stopwatch statsTimer = Stopwatch.createUnstarted();
    protected final Stopwatch cleanupTimer = Stopwatch.createUnstarted();

    protected long chunks = 0;
    protected long prevChunks = 0;
    protected int prevProgress = 0;
    protected boolean stopped = true;

    protected long rateCount = 0L;
    protected double totalRate = 0D;

    protected PreGenRegion.ChunkIterator chunkIterator = null;

    public AbstractTask(String name, PreGenConfig config) {
        List<PreGenRegion> regions = config.getRegions();
        this.name = name;
        this.config = config;
        this.regions = regions.iterator();
        this.chunkCount = (PreGenRegion.SIZE * PreGenRegion.SIZE * regions.size()) - (config.getChunkIndex() + 1);
    }

    public abstract void flushChunks();

    public abstract void setup();

    public abstract void process();

    public abstract void tearDown();

    public abstract File getConfigFile();

    @Override
    public String getName() {
        return name;
    }

    public boolean start() {
        if (stopped) {
            stopped = false;
            statsTimer.reset().start();
            cleanupTimer.reset().start();
            PreGen.getInstance().getScheduler().submit(this);
            Log.printf("(%s) Task started", name);
            return true;
        }
        return false;
    }

    public boolean pause() {
        if (!stopped) {
            stopped = true;
            statsTimer.stop().reset();
            cleanupTimer.stop().reset();
            IO.saveConfig(config, getConfigFile());
            Log.printf("(%s) Task paused", name);
            return true;
        }
        return false;
    }

    public boolean cancel() {
        stopped = true;
        IO.deleteConfig(getConfigFile());
        Log.printf("(%s) Task cancelled", name);
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
        return !chunkIterator.hasNext();
    }

    @Override
    public boolean perform() {
        tick();
        setup();
        process();
        tearDown();
        return true;
    }

    protected void tick() {
        // print progress stats every X seconds
        if (shouldPrintStats()) {
            printStats();
            IO.saveConfig(config, getConfigFile());
            statsTimer.reset().start();
        }
        // save world chunks to disk every X seconds, should also free up memory
        if (cleanupTimer.elapsed(TimeUnit.SECONDS) >= CLEANUP_INTERVAL) {
            flushChunks();
        }
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
}
