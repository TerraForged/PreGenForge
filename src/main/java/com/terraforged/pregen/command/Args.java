package com.terraforged.pregen.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.ILocationArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

import java.util.concurrent.CompletableFuture;

public class Args {

    public static ArgumentType<DimensionType> dim() {
        return wrap(DimensionArgument.getDimension());
    }

    public static ArgumentType<Integer> integer(int min) {
        return wrap(IntegerArgumentType.integer(min));
    }

    public static ArgumentType<ILocationArgument> vec2() {
        return wrap(Vec2Argument.vec2());
    }

    private static <T> ArgumentType<T> wrap(ArgumentType<T> type) {
        return new ArgumentType<T>() {
            @Override
            public T parse(StringReader reader) throws CommandSyntaxException {
                return type.parse(reader);
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return type.listSuggestions(context, builder);
            }
        };
    }
}
