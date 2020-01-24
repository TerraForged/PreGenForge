package com.terraforged.pregen.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.terraforged.pregen.Log;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Predicate;

public class CommandUtils {

    public static final int PERMISSION_LEVEL = 1;
    public static final Predicate<CommandSource> PERMISSION = CommandUtils::hasPermission;

    public static void send(CommandSource source, String message, Object... args) {
        try {
            // assert source is a player since the console gets log messages instead
            source.asPlayer();

            source.sendFeedback(Log.format(message, args), true);
        } catch (CommandSyntaxException ignored) {

        }
    }

    public static boolean hasPermission(CommandSource source) {
        if (!source.hasPermissionLevel(PERMISSION_LEVEL)) {
            source.sendErrorMessage(new StringTextComponent("No permission"));
            return false;
        }
        return true;
    }

    public static ServerWorld getWorld(CommandContext<CommandSource> context) throws CommandSyntaxException {
        DimensionType dimension = getDimension(context, "dimension");
        ServerWorld world = context.getSource().getServer().getWorld(dimension);
        if (world == null) {
            Message message = new LiteralMessage("Unable to get world for dimension: " + dimension);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
        return world;
    }

    public static DimensionType getDimension(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        try {
            // use the specified dimension type if provided
            return context.getArgument(name, DimensionType.class);
        } catch (IllegalArgumentException exception) {
            try {
                // if command was run by player, use their current dimension as a fallback
                return context.getSource().asPlayer().dimension;
            } catch (CommandSyntaxException e) {
                // otherwise use overworld by default
                return DimensionType.OVERWORLD;
            }
        }
    }
}
