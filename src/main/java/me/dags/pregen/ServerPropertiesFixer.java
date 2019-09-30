package me.dags.pregen;


import net.minecraft.server.ServerPropertiesProvider;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.ServerProperties;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerPropertiesFixer {

    @SubscribeEvent
    public static void setup(FMLDedicatedServerSetupEvent event) {
        try {
            DedicatedServer server = event.getServerSupplier().get();
            ServerPropertiesProvider settings = getSettings(server);
            if (settings == null) {
                return;
            }

            Path overridesPath = Paths.get("overrides.properties");
            Properties overrides = loadProperties(overridesPath);

            Path serverPath = Paths.get("server.properties");
            Properties properties = loadProperties(serverPath);

            if (overrides.isEmpty()) {
                overrides.setProperty("level-type", properties.getProperty("level-type"));
                overrides.setProperty("max-tick-time", properties.getProperty("max-tick-time"));
                saveProperties(overrides, overridesPath);
            } else {
                properties.putAll(overrides);
            }

            settings.func_219033_a(p -> new ServerProperties(properties));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ServerPropertiesProvider getSettings(DedicatedServer server) throws IllegalAccessException {
        for (Field field : DedicatedServer.class.getDeclaredFields()) {
            if (field.getType() == ServerPropertiesProvider.class) {
                field.setAccessible(true);
                return (ServerPropertiesProvider) field.get(server);
            }
        }
        return null;
    }

    private static Properties loadProperties(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    private static void saveProperties(Properties properties, Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            properties.store(writer, "Minecraft server property overrides");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}