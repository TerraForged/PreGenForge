package me.dags.pregen.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.dags.pregen.PreGenForge;
import me.dags.pregen.pregenerator.PreGenConfig;
import me.dags.pregen.pregenerator.PreGenRegion;
import me.dags.pregen.pregenerator.PreGenerator;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
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
                                        .executes(PreGenCommand::start)
                                        .then(Commands.argument("radius2", IntegerArgumentType.integer(1))
                                                .executes(PreGenCommand::startRange)))));
    }

    private static Predicate<CommandSource> predicate() {
        return source -> {
            if (!source.hasPermissionLevel(1)) {
                source.sendErrorMessage(new TextComponentString("No permission"));
                return false;
            }
            return true;
        };
    }

    private static int resume(CommandContext<CommandSource> context) {
        WorldServer worldServer = context.getSource().getWorld();
        PreGenForge.startGenerator(worldServer);
        return 0;
    }

    private static int pause(CommandContext<CommandSource> context) {
        WorldServer worldServer = context.getSource().getWorld();
        PreGenForge.pauseGenerator(worldServer);
        return 0;
    }

    private static int cancel(CommandContext<CommandSource> context) {
        WorldServer worldServer = context.getSource().getWorld();
        PreGenForge.cancelGenerator(worldServer);
        return 0;
    }

    private static int limit(CommandContext<CommandSource> context) {
        int limit = IntegerArgumentType.getInteger(context, "limit");
        WorldServer worldServer = context.getSource().getWorld();
        PreGenForge.getPreGenerator(worldServer).ifPresent(preGenerator -> preGenerator.setLimit(limit));
        return 0;
    }

    private static int start(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Vec2f center = Vec2Argument.getVec2f(context, "center");
        int radius = IntegerArgumentType.getInteger(context, "radius");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionRadius = PreGenRegion.chunkToRegion(radius);

        WorldServer world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenConfig config = new PreGenConfig(regionX, regionZ, regionRadius);
        PreGenerator generator = PreGenForge.createGenerator(world, config);
        generator.start();

        return 0;
    }

    private static int startRange(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Vec2f center = Vec2Argument.getVec2f(context, "center");
        int innerRadius = IntegerArgumentType.getInteger(context, "radius");
        int outerRadius = IntegerArgumentType.getInteger(context, "radius2");

        int regionX = PreGenRegion.blockToRegion((int) center.x);
        int regionZ = PreGenRegion.blockToRegion((int) center.y);
        int regionInnerRadius = PreGenRegion.chunkToRegion(innerRadius);
        int regionOuterRadius = PreGenRegion.chunkToRegion(outerRadius);
        int regionStart = 1 + ((1 + regionInnerRadius * 2) * (1 + regionInnerRadius * 2));

        PreGenConfig config = new PreGenConfig(regionX, regionZ, regionOuterRadius);
        config.setRegionIndex(regionStart);

        WorldServer world = context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
        PreGenerator generator = PreGenForge.createGenerator(world, config);
        generator.start();

        return 0;
    }
}
