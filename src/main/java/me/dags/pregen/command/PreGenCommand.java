package me.dags.pregen.command;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.dags.pregen.PreGenForge;
import me.dags.pregen.pregenerator.PreGenConfig;
import me.dags.pregen.pregenerator.PreGenRegion;
import me.dags.pregen.pregenerator.PreGenerator;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Predicate;

public class PreGenCommand {

    public static LiteralArgumentBuilder<CommandSource> command() {
        return Commands.literal("pregen")
                .requires(predicate())
                .then(Commands.literal("pause").executes(PreGenCommand::pause))
                .then(Commands.literal("resume").executes(PreGenCommand::resume))
                .then(Commands.literal("cancel").executes(PreGenCommand::cancel))
                .then(Commands.literal("limit")
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                .executes(PreGenCommand::limit)))
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
                            .executes(PreGenCommand::startRegion)));
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
        return 0;
    }

    private static int pause(CommandContext<CommandSource> context) {
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.pauseGenerator(worldServer);
        return 0;
    }

    private static int cancel(CommandContext<CommandSource> context) {
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.cancelGenerator(worldServer);
        return 0;
    }

    private static int limit(CommandContext<CommandSource> context) {
        int limit = IntegerArgumentType.getInteger(context, "limit");
        ServerWorld worldServer = context.getSource().getWorld();
        PreGenForge.getPreGenerator(worldServer).ifPresent(preGenerator -> preGenerator.setLimit(limit));
        return 0;
    }

    private static int start(CommandContext<CommandSource> context) throws CommandSyntaxException {
        PreGenForge.print(context.getInput());

        Vec2f center = Vec2Argument.getVec2f(context, "center");
        int radius = IntegerArgumentType.getInteger(context, "radius");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionRadius = PreGenRegion.chunkToRegion(radius);

        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenConfig config = new PreGenConfig(regionX, regionZ, regionRadius);
        PreGenerator generator = PreGenForge.createGenerator(world, config);
        generator.start();

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

        PreGenConfig config = new PreGenConfig(regionX, regionZ, Math.max(regionInnerRadius + 1, regionOuterRadius));
        config.setRegionIndex(regionStart);

        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenerator generator = PreGenForge.createGenerator(world, config);
        generator.start();

        return 0;
    }

    private static int startRegion(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Vec2f position = Vec2Argument.getVec2f(context, "position");
        int regionX = (int) position.x;
        int regionZ = (int) position.y;

        PreGenConfig config = new PreGenConfig(regionX, regionZ, 1);
        ServerWorld world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenerator generator = PreGenForge.createGenerator(world, config);
        generator.start();

        return 0;
    }
}
