package com.terraforged.pregen.util;

import com.terraforged.pregen.PreGen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class Log {

    public static ITextComponent format(String format, Object... args) {
        return new StringTextComponent("[PGF] " + String.format(format, args));
    }

    public static void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    public static void print(String... lines) {
        for (String line : lines) {
            print(new StringTextComponent("[PGF] " + line));
        }
    }

    public static void print(ITextComponent message) {
        PreGen.getInstance().getMessageSink().accept(message);
    }
}
