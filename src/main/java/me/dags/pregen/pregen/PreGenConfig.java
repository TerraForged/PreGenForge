package me.dags.pregen.pregen;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PreGenConfig {

    private final int x;
    private final int z;
    private final int radius;

    private int limit = 40;
    private int regionIndex = 0;
    private int chunkIndex = PreGenRegion.ITERATOR_INDEX;

    public PreGenConfig(int x, int z, int radius) {
        this.x = x;
        this.z = z;
        this.radius = radius;
    }

    public PreGenConfig(JsonObject root) {
        x = root.get("x").getAsInt();
        z = root.get("z").getAsInt();
        radius = root.get("radius").getAsInt();
        limit = root.get("limit").getAsInt();
        regionIndex = root.get("region").getAsInt();
        chunkIndex = root.get("chunk").getAsInt();
    }

    public int getRegionIndex() {
        return regionIndex;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isValid() {
        return radius > 0;
    }

    public List<PreGenRegion> getRegions() {
        List<PreGenRegion> regions = getRegions(x, z, radius);
        return regions.subList(regionIndex, regions.size());
    }

    public void setRegionIndex(int index) {
        regionIndex = index;
    }

    public void setChunkIndex(int index) {
        chunkIndex = index;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("x", x);
        root.addProperty("z", z);
        root.addProperty("radius", radius);
        root.addProperty("limit", limit);
        root.addProperty("region", regionIndex);
        root.addProperty("chunk", chunkIndex);
        return root;
    }

    public static List<PreGenRegion> getRegions(int centerX, int centerZ, int radius) {
        int x = 0;
        int y = 0;
        int dx = 0;
        int dy = -1;
        int size = radius + 1 + radius;
        int max = size * size;
        List<PreGenRegion> regions = new ArrayList<>(max);
        for(int i = 0; i < max; i++){
            if ((-radius <= x) && (x <= radius) && (-radius <= y) && (y <= radius)){
                int regionX = centerX + x;
                int regionZ = centerZ + y;
                regions.add(new PreGenRegion(regionX, regionZ));
            }
            if((x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1-y))) {
                size = dx;
                dx = -dy;
                dy = size;
            }
            x += dx;
            y += dy;
        }
        return regions;
    }
}
