package com.terraforged.pregen;

import com.terraforged.pregen.pregen.PreGenConfig;
import com.terraforged.pregen.pregen.PreGenTask;
import com.terraforged.pregen.task.AbstractTask;
import com.terraforged.pregen.task.TaskScheduler;
import com.terraforged.pregen.util.IO;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
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
    private final Map<String, AbstractTask> generators = new HashMap<>();
    private final AtomicBoolean notifyPlayers = new AtomicBoolean(false);

    private PreGen(MinecraftServer server, TaskScheduler scheduler) {
        this.server = server;
        this.scheduler = scheduler;
        this.messageSink = msg -> {
            if (notifyPlayers.get()) {
                server.getPlayerList().func_232641_a_(msg, ChatType.GAME_INFO, Util.DUMMY_UUID);
            } else {
                server.sendMessage(msg, Util.DUMMY_UUID);
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
        return String.format(format, name);
    }

    public Optional<AbstractTask> getTask(ServerWorld server) {
        return Optional.ofNullable(generators.get(PreGenTask.getName(server)));
    }

    public PreGenTask createTask(ServerWorld world, PreGenConfig config) {
        cancelTask(world);
        PreGenTask task = new PreGenTask(world, config);
        generators.put(task.getName(), task);
        return task;
    }

    public void startTask(ServerWorld world) {
        getTask(world).ifPresent(AbstractTask::start);
    }

    public void pauseTask(ServerWorld world) {
        getTask(world).ifPresent(AbstractTask::pause);
    }

    public void cancelTask(ServerWorld world) {
        getTask(world).ifPresent(task -> {
            task.cancel();
            generators.remove(task.getName());
        });
    }

    public void onStartup() {
        for (ServerWorld world : server.getWorlds()) {
            File file = IO.getConfigFile(PreGenTask.getName(world));
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
