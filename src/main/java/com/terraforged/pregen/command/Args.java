package com.terraforged.pregen.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.ILocationArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.util.ResourceLocation;

public class Args {

    public static ArgumentType<ResourceLocation> dim() {
        return DimensionArgument.getDimension();
    }

    public static ArgumentType<Integer> integer(int min) {
        return IntegerArgumentType.integer(min);
    }

    public static ArgumentType<ILocationArgument> vec2() {
        return Vec2Argument.vec2();
    }
}
