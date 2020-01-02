package me.dags.pregen;

import me.dags.pregen.pregen.PreGenConfig;
import me.dags.pregen.pregen.PreGenTask;
import me.dags.pregen.task.TaskScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PreGen {

    private static PreGen instance = null;

    private final MinecraftServer server;
    private final TaskScheduler scheduler;
    private final Consumer<ITextComponent> messageSink;
    private final Map<String, PreGenTask> generators = new HashMap<>();
    private final AtomicBoolean notifyPlayers = new AtomicBoolean(false);

    private PreGen(MinecraftServer server, TaskScheduler scheduler) {
        this.server = server;
        this.scheduler = scheduler;
        this.messageSink = msg -> {
            if (notifyPlayers.get()) {
                server.getPlayerList().sendMessage(msg, true);
            } else {
                server.sendMessage(msg);
            }
        };
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public Consumer<ITextComponent> getMessageSink() {
        return messageSink;
    }

    public String setPlayerNotifications(boolean state) {
        boolean previous = notifyPlayers.getAndSet(state);
        boolean change = previous != state;
        String name = state ? "enabled" : "disabled";
        String format = change ? "Player notifications: %s" : "Player notifications already: %s";
        String message = String.format(format, name);
        Log.print(message);
        return message;
    }

    public Optional<PreGenTask> getTask(ServerWorld server) {
        return Optional.ofNullable(generators.get(PreGenTask.getName(server)));
    }

    public PreGenTask createTask(ServerWorld world, PreGenConfig config) {
        cancelTask(world);
        PreGenTask task = new PreGenTask(world, config);
        generators.put(task.getName(), task);
        return task;
    }

    public void startTask(ServerWorld world) {
        getTask(world).ifPresent(PreGenTask::start);
    }

    public void pauseTask(ServerWorld world) {
        getTask(world).ifPresent(PreGenTask::pause);
    }

    public void cancelTask(ServerWorld world) {
        getTask(world).ifPresent(task -> {
            task.cancel();
            generators.remove(task.getName());
        });
    }

    public void onStartup() {
        for (ServerWorld world : server.getWorlds()) {
            File file = IO.getConfigFile(world);
            if (file.exists()) {
                try {
                    PreGenConfig config = IO.loadConfig(file);
                    PreGenTask task = createTask(world, config);
                    task.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onShutdown() {
        for (ServerWorld worldServer : server.getWorlds()) {
            pauseTask(worldServer);
        }
    }

    public static void init(MinecraftServer server, TaskScheduler scheduler) {
        instance = new PreGen(server, scheduler);
    }

    public static PreGen getInstance() {
        if (instance == null) {
            throw new NullPointerException("PreGen has not been initialized!");
        }
        return instance;
    }
}
