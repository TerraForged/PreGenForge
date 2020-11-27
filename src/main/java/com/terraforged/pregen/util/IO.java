package com.terraforged.pregen.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.terraforged.pregen.pregen.PreGenConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class IO {

    public static void saveConfig(PreGenConfig config, File file) {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            Log.printf("Unable to create parent directory: %s", file.getParent());
            return;
        }

        try {
            if (!file.exists() && !file.createNewFile()) {
                Log.printf("Unable to create file: %s", file.getPath());
                return;
            }

            try (FileWriter writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson(), writer);
                writer.flush();
            }
        } catch (Throwable t) {
            t.printStackTrace();
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

    public static boolean deleteConfig(File file) {
        return file.exists() && file.delete();
    }

    public static File getFolder() {
        File file = new File("pregen").getAbsoluteFile();
        file.mkdirs();
        return file;
    }

    public static File getConfigFile(String name) {
        return new File(getFolder(), name + ".json");
    }
}
