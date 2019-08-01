package me.dags.pregen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.dags.pregen.command.PreGenCommand;
import me.dags.pregen.pregenerator.PreGenConfig;
import me.dags.pregen.pregenerator.PreGenerator;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.ServerWorld;
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
    private static Consumer<ITextComponent> messageSink = t -> {
    };

    @SubscribeEvent
    public static void starting(FMLServerStartingEvent event) {
        messageSink = event.getServer()::sendMessage;

        event.getCommandDispatcher().register(PreGenCommand.command());

        for (ServerWorld worldServer : event.getServer().getWorlds()) {
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
        for (ServerWorld worldServer : event.getServer().getWorlds()) {
            pauseGenerator(worldServer);
        }
    }

    public static void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    public static void print(String... lines) {
        for (String line : lines) {
            print(new StringTextComponent("[PreGen] " + line));
        }
    }

    private static void print(ITextComponent message) {
        messageSink.accept(message);
    }

    public static Optional<PreGenerator> getPreGenerator(ServerWorld server) {
        return Optional.ofNullable(generators.get(server.getWorldInfo().getWorldName()));
    }

    public static PreGenerator createGenerator(ServerWorld worldServer, PreGenConfig config) {
        cancelGenerator(worldServer);
        PreGenerator generator = new PreGenerator(worldServer, config);
        generators.put(worldServer.getWorldInfo().getWorldName(), generator);
        return generator;
    }

    public static void startGenerator(ServerWorld worldServer) {
        getPreGenerator(worldServer).ifPresent(PreGenerator::start);
    }

    public static void pauseGenerator(ServerWorld worldServer) {
        getPreGenerator(worldServer).ifPresent(PreGenerator::pause);
    }

    public static void cancelGenerator(ServerWorld worldServer) {
        getPreGenerator(worldServer).ifPresent(gen -> {
            gen.cancel();
            generators.remove(gen.getName());
        });
    }

    public static void savePreGenerator(ServerWorld server, PreGenConfig config) {
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
            printf("Unable to create parent directory: %s", file.getParent());
            return;
        }

        if (!file.exists() && !file.createNewFile()) {
            printf("Unable to create file: %s", file.getPath());
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

    public static boolean deletePreGenerator(ServerWorld server) {
        File file = getConfigFile(server);
        return file.exists() && file.delete();
    }

    private static File getConfigFile(ServerWorld server) {
        return new File(server.getSaveHandler().getWorldDirectory().getAbsoluteFile(), "pregen.json");
    }
}
