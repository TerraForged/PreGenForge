package com.terraforged.pregen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.terraforged.pregen.pregen.PreGenConfig;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class IO {

    public static void saveConfig(PreGenConfig config, ServerWorld server) {
        if (!config.isValid()) {
            return;
        }

        try {
            File file = getConfigFile(server);
            saveConfig(config, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig(PreGenConfig config, File file) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            Log.printf("Unable to create parent directory: %s", file.getParent());
            return;
        }

        if (!file.exists() && !file.createNewFile()) {
            Log.printf("Unable to create file: %s", file.getPath());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson(), writer);
            writer.flush();
        }
    }

    public static PreGenConfig loadConfig(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonElement element = new JsonParser().parse(reader);
            if (element.isJsonObject()) {
                return new PreGenConfig(element.getAsJsonObject());
            }
            throw new IOException("invalid pregen file");
        }
    }

    public static boolean deleteConfig(ServerWorld server) {
        File file = getConfigFile(server);
        return file.exists() && file.delete();
    }

    public static File getFolder(ServerWorld world) {
        return world.getDimension().getType().getDirectory(world.getSaveHandler().getWorldDirectory());
    }

    public static File getConfigFile(ServerWorld server) {
        return new File(getFolder(server), "pregen.json");
    }
}
