package me.dags.pregen.task;

public interface TaskScheduler {

    // called at the start of the server tick
    void startTick();

    // called at the end of the server tick
    // this is where tasks should be performed using whatever remaining time there is
    void endTick();

    // called during server shutdown. dispose of any tasks, do any cleanup etc
    void clear();

    // queue a task to be run during a tick
    void submit(Task task);
}
