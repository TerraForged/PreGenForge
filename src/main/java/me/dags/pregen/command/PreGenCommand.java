package me.dags.pregen.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.dags.pregen.PreGenForge;
import me.dags.pregen.pregenerator.PreGenConfig;
import me.dags.pregen.pregenerator.PreGenRegion;
import me.dags.pregen.pregenerator.PreGenWorker;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Predicate;

public class PreGenCommand {

    public static LiteralArgumentBuilder<CommandSource> command() {
        return Commands.literal("pregen")
                .requires(predicate())
                .then(Commands.literal("pause").executes(PreGenCommand::pause))
                .then(Commands.literal("resume").executes(PreGenCommand::resume))
                .then(Commands.literal("cancel").executes(PreGenCommand::cancel))
                .then(Commands.literal("notify")
                        .then(Commands.argument("state", BoolArgumentType.bool()))
                            .executes(PreGenCommand::notify))
                .then(Commands.literal("start")
                        .then(Commands.argument("center", Vec2Argument.vec2())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                        .executes(PreGenCommand::start))))
                .then(Commands.literal("expand")
                        .then(Commands.argument("center", Vec2Argument.vec2())
                                .then(Commands.argument("innerRadius", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("outerRadius", IntegerArgumentType.integer(1))
                                                .executes(PreGenCommand::expand)))))
                .then(Commands.literal("region")
                        .then(Commands.argument("position", Vec2Argument.vec2())
                            .executes(PreGenCommand::startRegion)))
                .then(Commands.literal("time")
                        .then(Commands.argument("ticks", LongArgumentType.longArg(-1))
                            .executes(PreGenCommand::time)));
    }

    private static Predicate<CommandSource> predicate() {
        return source -> {
            if (!source.hasPermissionLevel(1)) {
                source.sendErrorMessage(new StringTextComponent("No permission"));
                return false;
            }
            return true;
        };
    }

    private static int resume(CommandContext<CommandSource> context) {
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.startGenerator(worldServer);
        send(context.getSource(), "Pregenerator resumed");
        return 0;
    }

    private static int pause(CommandContext<CommandSource> context) {
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.pauseGenerator(worldServer);
        send(context.getSource(), "Pregenerator paused");
        return 0;
    }

    private static int cancel(CommandContext<CommandSource> context) {
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.cancelGenerator(worldServer);
        send(context.getSource(), "Pregenerator cancelled");
        return 0;
    }

    private static int notify(CommandContext<CommandSource> context) {
        boolean state = BoolArgumentType.getBool(context, "notify");
        boolean result = PreGenForge.setPlayerNotifications(state);
        String name = state ? "enabled" : "disabled";
        String format = result ? "Player notifications: %s" : "Player notifications already: %s";
        ITextComponent message = PreGenForge.format(format, name);
        context.getSource().sendFeedback(message, true);
        return 0;
    }

    private static int time(CommandContext<CommandSource> context) {
        long ticks = LongArgumentType.getLong(context, "ticks");
        ServerWorld worldServer = context.getSource().getWorld();
        worldServer.setGameTime(ticks);
        worldServer.setDayTime(ticks);
        send(context.getSource(), "Set world & game time: %s", ticks);
        return 0;
    }

    private static int start(CommandContext<CommandSource> context) throws CommandSyntaxException {
        PreGenForge.print(context.getInput());

        Vec2f center = Vec2Argument.getVec2f(context, "center");
        int radius = IntegerArgumentType.getInteger(context, "radius");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionRadius = PreGenRegion.chunkToRegion(radius);
        PreGenConfig config = new PreGenConfig(regionX, regionZ, regionRadius);
        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);

        PreGenWorker worker = PreGenForge.createGenerator(world, config);
        worker.start();

        send(context.getSource(), "Pregenerator started");

        return 0;
    }

    private static int expand(CommandContext<CommandSource> context) throws CommandSyntaxException {
        PreGenForge.print(context.getInput());

        Vec2f center = Vec2Argument.getVec2f(context, "center");
        int innerRadius = IntegerArgumentType.getInteger(context, "innerRadius");
        int outerRadius = IntegerArgumentType.getInteger(context, "outerRadius");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionInnerRadius = PreGenRegion.chunkToRegion(innerRadius);
        int regionOuterRadius = PreGenRegion.chunkToRegion(outerRadius);
        int regionStart = (1 + regionInnerRadius * 2) * (1 + regionInnerRadius * 2);

        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenConfig config = new PreGenConfig(regionX, regionZ, Math.max(regionInnerRadius + 1, regionOuterRadius));
        config.setRegionIndex(regionStart);

        PreGenWorker worker = PreGenForge.createGenerator(world, config);
        worker.start();

        send(context.getSource(), "Pregenerator started");
        return 0;
    }

    private static int startRegion(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Vec2f position = Vec2Argument.getVec2f(context, "position");
        int regionX = (int) position.x;
        int regionZ = (int) position.y;

        PreGenConfig config = new PreGenConfig(regionX, regionZ, 1);
        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenWorker worker = PreGenForge.createGenerator(world, config);
        worker.start();

        send(context.getSource(), "Pregenerator started");
        return 0;
    }

    private static void send(CommandSource source, String message, Object... args) {
        try {
            source.asPlayer();
            source.sendFeedback(PreGenForge.format(message, args), true);
        } catch (CommandSyntaxException ignored) {

        }
    }

    private static int chunkRadiusToBlockDiam(int radius) {
        int regionRad = radius >> 5;
        int regionDiam = regionRad + 1 + regionRad;
        int chunkDiam = regionDiam << 5;
        return chunkDiam << 4;
    }
}
