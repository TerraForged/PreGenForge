package com.terraforged.pregen.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.terraforged.pregen.util.Log;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;

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

    public static void sendToAny(CommandSource source, String message, Object... args) {
        source.sendFeedback(Log.format(message, args), true);
    }

    public static boolean hasPermission(CommandSource source) {
        if (!source.hasPermissionLevel(PERMISSION_LEVEL)) {
            source.sendErrorMessage(new StringTextComponent("No permission"));
            return false;
        }
        return true;
    }

    public static Vec2f getCenter(CommandContext<CommandSource> context, ServerWorld world) {
        try {
            return context.getArgument("center", Vec2f.class);
        } catch (Throwable t) {
            try {
                PlayerEntity player = context.getSource().asPlayer();
                return new Vec2f(player.getPosition().getX(), player.getPosition().getZ());
            } catch (CommandSyntaxException e) {
                WorldInfo info = world.getWorldInfo();
                return new Vec2f(info.getSpawnX(), info.getSpawnZ());
            }
        }
    }

    public static ServerWorld getWorld(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerWorld world = getDimension(context, "dimension");
        if (world == null) {
            Message message = new LiteralMessage("Unable to get dimension");
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
        return world;
    }

    public static ServerWorld getDimension(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        try {
            DimensionType dimensionType = DimensionArgument.getDimensionArgument(context, name);
            // use the specified dimension type if provided
            return context.getSource().getServer().getWorld(dimensionType);
        } catch (IllegalArgumentException exception) {
            try {
                // if command was run by player, use their current dimension as a fallback
                return context.getSource().asPlayer().getServerWorld();
            } catch (CommandSyntaxException e) {
                // otherwise use overworld by default
                return context.getSource().getServer().getWorld(DimensionType.OVERWORLD);
            }
        }
    }
}
