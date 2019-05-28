package me.dags.pregen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.dags.pregen.command.PreGenCommand;
import me.dags.pregen.pregenerator.PreGenConfig;
import me.dags.pregen.pregenerator.PreGenerator;
import net.minecraft.network.rcon.IServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Mod("pregenforge")
@Mod.EventBusSubscriber
public class PreGenForge {

    private static final Map<String, PreGenerator> generators = new HashMap<>();
    private static Consumer<ITextComponent> messageSink = t -> {};

    @SubscribeEvent
    public static void starting(FMLServerStartingEvent event) {
        event.getCommandDispatcher().register(PreGenCommand.command());

        if (event.getServer() instanceof IServer) {
            ((IServer) event.getServer()).setProperty("max-tick-time", -1);
        }
    }

    @SubscribeEvent
    public static void started(FMLServerStartingEvent event) {
        messageSink = event.getServer()::sendMessage;

        for (WorldServer worldServer : event.getServer().getWorlds()) {
            File file = getConfigFile(worldServer);
            if (file.exists()) {
                try {
                    PreGenConfig config = loadConfig(file);
                    createGenerator(worldServer, config).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopping(FMLServerStoppingEvent event) {
        for (WorldServer worldServer : event.getServer().getWorlds()) {
            pauseGenerator(worldServer);
        }
    }

    public static void print(String... lines) {
        for (String line : lines) {
            print(new TextComponentString(line));
        }
    }

    public static void print(ITextComponent message) {
        messageSink.accept(message);
    }

    public static Optional<PreGenerator> getPreGenerator(WorldServer server) {
        return Optional.ofNullable(generators.get(server.getWorldInfo().getWorldName()));
    }

    public static PreGenerator createGenerator(WorldServer worldServer, PreGenConfig config) {
        cancelGenerator(worldServer);
        PreGenerator generator = new PreGenerator(worldServer, config);
        generators.put(worldServer.getWorldInfo().getWorldName(), generator);
        return generator;
    }

    public static void startGenerator(WorldServer worldServer) {
        getPreGenerator(worldServer).ifPresent(PreGenerator::start);
    }

    public static void pauseGenerator(WorldServer worldServer) {
        getPreGenerator(worldServer).ifPresent(PreGenerator::pause);
    }

    public static void cancelGenerator(WorldServer worldServer) {
        getPreGenerator(worldServer).ifPresent(PreGenerator::cancel);
    }

    public static void savePreGenerator(WorldServer server, PreGenConfig config) {
        if (!config.isValid()) {
            return;
        }

        File file = getConfigFile(server);
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                print("Unable to create parent directory: " + file.getParent());
                return;
            }

            if (!file.exists() && !file.createNewFile()) {
                print("Unable to create file: " + file.getPath());
                return;
            }

            try (FileWriter writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson(), writer);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig(PreGenConfig config, File file) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            print("Unable to create parent directory: " + file.getParent());
            return;
        }

        if (!file.exists() && !file.createNewFile()) {
            print("Unable to create file: " + file.getPath());
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

    public static void deletePreGenerator(WorldServer server) {
        File file = getConfigFile(server);
        if (file.exists() && file.delete()) {
            print("Deleted pre-generator for world: " + server.getWorldInfo().getWorldName());
        }
    }

    private static File getConfigFile(WorldServer server) {
        return new File(server.getChunkSaveLocation().getAbsoluteFile(), "pregen.json");
    }
}
