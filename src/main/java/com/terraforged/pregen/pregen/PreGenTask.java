package com.terraforged.pregen.pregen;

import com.terraforged.pregen.task.AbstractTask;
import com.terraforged.pregen.util.IO;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.io.File;

public class PreGenTask extends AbstractTask {

    protected final ServerWorld world;
    protected final ChunkPos[] queue = new ChunkPos[PreGenRegion.SIZE * PreGenRegion.SIZE];

    public PreGenTask(ServerWorld world, PreGenConfig config) {
        super(getName(world), config);
        this.world = world;
    }

    @Override
    public void flushChunks() {
        statsTimer.stop();
        world.getServer().save(true, true, true);
        driveTasks();
        System.gc();
        cleanupTimer.reset().start();
        statsTimer.start();
    }

    @Override
    public void setup() {
        world.getChunkProvider().getLightManager().func_215598_a(queue.length);
    }

    @Override
    public void process() {
        ServerWorld world = this.world;
        ChunkPos[] queue = this.queue;
        PreGenRegion.ChunkIterator iterator = chunkIterator;

        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            world.getChunkProvider().forceChunk(pos, true);
            queue[iterator.index()] = pos;
        }

        while (hasWork()) {
            driveTasks();
        }

        for (int i = 0; i < queue.length; i++) {
            world.getChunkProvider().forceChunk(queue[i], false);
            queue[i] = null;
            chunks++;
        }

        driveTasks();
    }

    @Override
    public void tearDown() {
        world.getChunkProvider().getLightManager().func_215598_a(5);
    }

    @Override
    public File getConfigFile() {
        return IO.getConfigFile(getName());
    }

    private boolean hasWork() {
        ServerChunkProvider chunkProvider = world.getChunkProvider();
        for (ChunkPos pos : queue) {
            if (chunkProvider.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                return true;
            }
        }
        return false;
    }

    private void driveTasks() {
        ServerWorld world = this.world;
        while (world.getServer().driveOne()) {

        }
        while (world.getChunkProvider().driveOneTask()) {

        }
        world.getServer().driveUntil(taskTimer.next(10));
    }

    public static String getName(ServerWorld world) {
        String name = world.getWorldInfo().getWorldName();
        DimensionType dimensionType = world.dimension.getType();
        ResourceLocation dim = dimensionType.getRegistryName();
        return name + "-" + dim.getPath();
    }
}
