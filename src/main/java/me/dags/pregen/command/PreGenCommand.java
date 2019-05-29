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
                .then(Commands.literal("start").executes(PreGenCommand::start))
                .then(Commands.literal("pause").executes(PreGenCommand::pause))
                .then(Commands.literal("cancel").executes(PreGenCommand::cancel))
                .then(Commands.literal("limit")
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                .executes(PreGenCommand::limit)))
                .then(Commands.literal("create")
                        .then(Commands.argument("center", Vec2Argument.vec2())
                                .then(Commands.argument("radius", IntegerArgumentType.integer())
                                        .executes(PreGenCommand::create))));
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

    private static int start(CommandContext<CommandSource> context) {
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
        PreGenerator.limiter = IntegerArgumentType.getInteger(context, "limit");
        PreGenForge.print("Set limit: " + PreGenerator.limiter);
        return 0;
    }

    private static int create(CommandContext<CommandSource> context) throws CommandSyntaxException {
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
}
