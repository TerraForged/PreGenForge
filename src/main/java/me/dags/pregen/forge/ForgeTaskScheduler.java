package me.dags.pregen.forge;

import me.dags.pregen.task.Task;
import me.dags.pregen.task.TaskQueue;
import me.dags.pregen.task.TaskScheduler;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * Sort-of a wrapper around forge's WorldWorkManager.
 *
 * Forge's manager behaves in one of two ways depending on the return value of the task's doWork() method:
 * - true - the task is re-used until it is complete, or returns false (then moves to the next task)
 * - false - the next task in the list is chosen until the end of the list is met
 *
 * In the first case tasks are executed in the order they are queued.
 * In the second case multiple tasks may be worked on, but only ever once each per tick - ie the available
 * work time may not be fully utilized.
 *
 *
 * This implementation offers a new task with each call to doWork() but cycles through the task list multiple
 * times until the work time runs out.
 */
public class ForgeTaskScheduler implements TaskScheduler, WorldWorkerManager.IWorker {

    private final TaskQueue scheduler = new TaskQueue();

    public ForgeTaskScheduler() {
        WorldWorkerManager.addWorker(this);
    }

    // handled by forge
    @Override
    public void startTick() {}

    // handled by forge
    @Override
    public void endTick() {}

    // handled by forge
    @Override
    public void clear() {}

    @Override
    public synchronized void submit(Task task) {
        scheduler.add(task);
    }

    @Override
    public synchronized boolean doWork() {
        return scheduler.perform();
    }

    @Override
    public boolean hasWork() {
        // always return true so forge doesn't dispose us
        return true;
    }
}
