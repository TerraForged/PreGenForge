package com.terraforged.pregen.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.pregen.util.Log;
import com.terraforged.pregen.PreGen;
import com.terraforged.pregen.pregen.PreGenConfig;
import com.terraforged.pregen.pregen.PreGenRegion;
import com.terraforged.pregen.pregen.PreGenTask;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PreGenCommand {

    private static final int MIN_RADIUS = 1;

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        registerUtils(dispatcher);
        registerStart(dispatcher);
        registerExpand(dispatcher);
    }

    private static void registerStart(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("pregen")
                .then(Commands.literal("start")
                        .requires(CommandUtils.PERMISSION)
                        .then(Commands.argument("radius", Args.integer(MIN_RADIUS))
                                // pregen start <radius> <dimension>
                                .then(Commands.argument("dimension", Args.dim())
                                        .executes(PreGenCommand::start))
                                // pregen start <radius> <center> <?dimension>
                                .then(Commands.argument("center", Args.vec2())
                                        .executes(PreGenCommand::start)
                                        .then(Commands.argument("dimension", Args.dim())
                                                .executes(PreGenCommand::start)))
                                // pregen start <radius>
                                .executes(PreGenCommand::start))));
    }

    private static void registerExpand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("pregen")
                .then(Commands.literal("expand")
                        .requires(CommandUtils.PERMISSION)
                        .then(Commands.argument("innerRadius", Args.integer(MIN_RADIUS))
                                .then(Commands.argument("outerRadius", Args.integer(MIN_RADIUS))
                                        .then(Commands.argument("center", Args.vec2())
                                                .then(Commands.argument("dimension", Args.dim())
                                                        .executes(PreGenCommand::expand))
                                                .executes(PreGenCommand::expand))
                                        .then(Commands.argument("dimension", Args.dim())
                                                .executes(PreGenCommand::expand))
                                        .executes(PreGenCommand::expand)))));
    }

    private static void registerUtils(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("pregen")
                .requires(CommandUtils.PERMISSION)
                // toggles progress notifications for players
                .then(Commands.literal("notify")
                        .then(Commands.argument("state", BoolArgumentType.bool())
                                .executes(PreGenCommand::notify)))
                // pauses a pregen task
                .then(Commands.literal("pause")
                        .executes(PreGenCommand::pause)
                        .then(Commands.argument("dimension", Args.dim())
                                .executes(PreGenCommand::pause)))
                // resumes a pregen task
                .then(Commands.literal("resume")
                        .executes(PreGenCommand::resume)
                        .then(Commands.argument("dimension", Args.dim())
                                .executes(PreGenCommand::resume)))
                // cancels & delete a pregen task
                .then(Commands.literal("cancel")
                        .executes(PreGenCommand::cancel)
                        .then(Commands.argument("dimension", Args.dim())
                                .executes(PreGenCommand::cancel)))
                // pregens a specific region
                .then(Commands.literal("region")
                        .then(Commands.argument("center", Args.vec2())
                                .executes(PreGenCommand::startRegion)
                                .then(Commands.argument("dimension", Args.dim())
                                        .executes(PreGenCommand::startRegion))))
                // sets the world game and day time
                .then(Commands.literal("time")
                        .then(Commands.argument("ticks", LongArgumentType.longArg(-1))
                                .executes(PreGenCommand::startRegion)
                                .then(Commands.argument("dimension", Args.dim())
                                        .executes(PreGenCommand::time)))));
    }

    private static int resume(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerWorld worldServer = CommandUtils.getWorld(context);
        PreGen.getInstance().startTask(worldServer);
        CommandUtils.send(context.getSource(), "Pregenerator resumed");
        return Command.SINGLE_SUCCESS;
    }

    private static int pause(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerWorld worldServer = CommandUtils.getWorld(context);
        PreGen.getInstance().pauseTask(worldServer);
        CommandUtils.send(context.getSource(), "Pregenerator paused");
        return Command.SINGLE_SUCCESS;
    }

    private static int cancel(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerWorld worldServer = CommandUtils.getWorld(context);
        PreGen.getInstance().cancelTask(worldServer);
        CommandUtils.send(context.getSource(), "Pregenerator cancelled");
        return Command.SINGLE_SUCCESS;
    }

    private static int notify(CommandContext<CommandSource> context) {
        boolean state = BoolArgumentType.getBool(context, "state");
        String response = PreGen.getInstance().setPlayerNotifications(state);
        CommandUtils.sendToAny(context.getSource(), response);
        return Command.SINGLE_SUCCESS;
    }

    private static int time(CommandContext<CommandSource> context) throws CommandSyntaxException {
        long ticks = LongArgumentType.getLong(context, "ticks");
        ServerWorld worldServer = CommandUtils.getWorld(context);
        IServerWorldInfo info = (IServerWorldInfo) worldServer.getWorldInfo();
        info.setGameTime(ticks);
        info.setDayTime(ticks);
        CommandUtils.send(context.getSource(), "Set world & game time: %s", ticks);
        return Command.SINGLE_SUCCESS;
    }

    // generates a square
    private static int start(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Log.print(context.getInput());
        ServerWorld world = CommandUtils.getWorld(context);
        Vector2f center = CommandUtils.getCenter(context, world);
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionRadius = PreGenRegion.chunkToRegion(radius);
        PreGenConfig config = new PreGenConfig(regionX, regionZ, regionRadius);
        PreGenTask worker = PreGen.getInstance().createTask(world, config);
        worker.start();
        CommandUtils.send(context.getSource(), "Pregenerator started");
        CommandUtils.send(context.getSource(), "Use '/pregen notify true' to show periodic progress updates");
        CommandUtils.send(context.getSource(), "Expect server lag & commands to take a few seconds to process");
        return Command.SINGLE_SUCCESS;
    }

    // generates a donut
    private static int expand(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Log.print(context.getInput());

        ServerWorld world = CommandUtils.getWorld(context);
        Vector2f center = CommandUtils.getCenter(context, world);
        int innerRadius = IntegerArgumentType.getInteger(context, "innerRadius");
        int outerRadius = IntegerArgumentType.getInteger(context, "outerRadius");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionInnerRadius = PreGenRegion.chunkToRegion(innerRadius);
        int regionOuterRadius = PreGenRegion.chunkToRegion(outerRadius);
        int regionStart = (1 + regionInnerRadius * 2) * (1 + regionInnerRadius * 2);

        PreGenConfig config = new PreGenConfig(regionX, regionZ, Math.max(regionInnerRadius + 1, regionOuterRadius));
        config.setRegionIndex(regionStart);

        PreGenTask worker = PreGen.getInstance().createTask(world, config);
        worker.start();

        CommandUtils.send(context.getSource(), "Pregenerator started");
        CommandUtils.send(context.getSource(), "Use '/pregen notify true' to show periodic progress updates");
        CommandUtils.send(context.getSource(), "Expect server lag & commands to take a few seconds to process");
        return Command.SINGLE_SUCCESS;
    }

    // generates one region file
    private static int startRegion(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Log.print(context.getInput());

        Vector2f position = Vec2Argument.getVec2f(context, "position");
        int regionX = (int) position.x;
        int regionZ = (int) position.y;

        PreGenConfig config = new PreGenConfig(regionX, regionZ, 1);
        ServerWorld world = CommandUtils.getWorld(context);
        PreGenTask worker = PreGen.getInstance().createTask(world, config);
        worker.start();

        CommandUtils.send(context.getSource(), "Pregenerator started");
        return Command.SINGLE_SUCCESS;
    }
}
