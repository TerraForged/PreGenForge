package me.dags.pregen.pregenerator;

import net.minecraft.util.math.ChunkPos;

public class PreGenRegion {

    public static final int SIZE = 32;
    public static final int ITERATOR_INDEX = -1;

    private final int x;
    private final int z;

    public PreGenRegion(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkIterator iterator() {
        return iterator(ITERATOR_INDEX);
    }

    public ChunkIterator iterator(int index) {
        return new ChunkIterator(index);
    }

    public class ChunkIterator {

        private static final int MAX_INDEX = SIZE * SIZE;

        private final int chunkX = regionToChunk(x);
        private final int chunkZ = regionToChunk(z);

        private int index;

        private ChunkIterator(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        public boolean hasNext() {
            return index + 1 < MAX_INDEX;
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
