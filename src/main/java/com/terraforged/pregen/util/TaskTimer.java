package com.terraforged.pregen.util;

import java.util.function.BooleanSupplier;

public class TaskTimer implements BooleanSupplier {

    private long time;

    public TaskTimer next(long intervalMs) {
        this.time = System.currentTimeMillis() + intervalMs;
        return this;
    }

    @Override
    public boolean getAsBoolean() {
        return System.currentTimeMillis() > time;
    }
}
