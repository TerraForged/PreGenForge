package com.terraforged.pregen.task;

public interface Task {

    // whether the task has finished or not
    boolean isComplete();

    // carries out the task's logic
    // returns true if it should repeat within a single tick (time allowing)
    boolean perform();
}
