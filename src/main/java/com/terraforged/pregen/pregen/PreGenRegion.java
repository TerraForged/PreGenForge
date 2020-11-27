package com.terraforged.pregen.pregen;

import net.minecraft.util.math.ChunkPos;

import java.util.function.LongConsumer;

public class PreGenRegion {

    public static final int SIZE = 32;
    public static final int DEFAULT_INDEX = -1;

    private final int x;
    private final int z;

    public PreGenRegion(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getStartChunkX() {
        return regionToChunk(x);
    }

    public int getStartChunkZ() {
        return regionToChunk(z);
    }

    public ChunkIterator iterator() {
        return iterator(DEFAULT_INDEX);
    }

    public ChunkIterator iterator(int index) {
        return new ChunkIterator(index);
    }

    public void forEach(LongConsumer consumer) {
        int chunkX = regionToChunk(x);
        int chunkZ = regionToChunk(z);
        for (int i = 0; i < ChunkIterator.MAX_INDEX; i++) {
            int dz = i / SIZE;
            int dx = i - (dz * SIZE);
            int cx = chunkX + dx;
            int cz = chunkZ + dz;
            consumer.accept(ChunkPos.asLong(cx, cz));
        }
    }

    public class ChunkIterator {

        private static final int MAX_INDEX = SIZE * SIZE;

        private final int chunkX = regionToChunk(x);
        private final int chunkZ = regionToChunk(z);

        private int index;

        private ChunkIterator(int index) {
            this.index = index;
        }

        public void reset() {
            index = DEFAULT_INDEX;
        }

        public int index() {
            return index;
        }

        public boolean hasNext() {
            return index + 1 < MAX_INDEX;
        }

        public ChunkPos center() {
            return new ChunkPos(chunkX + 16, chunkZ + 16);
        }

        public ChunkPos next() {
            index++;
            int dz = index / SIZE;
            int dx = index - (dz * SIZE);
            return new ChunkPos(chunkX + dx, chunkZ + dz);
        }

    }

    public static int blockToChunk(int blockCoord) {
        return blockCoord >> 4;
    }

    public static int chunkToBlock(int chunkCoord) {
        return chunkCoord << 4;
    }

    public static int chunkToRegion(int chunkCoord) {
        return chunkCoord >> 5;
    }

    public static int regionToChunk(int regionCoord) {
        return regionCoord << 5;
    }

    public static int blockToRegion(int blockCoord) {
        return chunkToRegion(blockToChunk(blockCoord));
    }

    public static int regionToBlock(int regionCoord) {
        return chunkToBlock(regionToChunk(regionCoord));
    }
}
