package me.dags.pregen.task;

// Tweaked version of forge's
// https://github.com/MinecraftForge/MinecraftForge/blob/1.15.x/src/main/java/net/minecraftforge/common/WorldWorkerManager.java
public class WorldTaskScheduler implements TaskScheduler {

    public static final TaskScheduler INSTANCE = new WorldTaskScheduler();

    private final TaskQueue taskQueue = new TaskQueue();

    private long startTime = -1;

    @Override
    public synchronized void startTick() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void endTick() {
        final long now = System.currentTimeMillis();

        long time = 50 - (now - startTime);
        if (time < 10) {
            // If ticks are lagging, give us at least 10ms to do something.
            time = 10;
        }

        time += now;

        while (System.currentTimeMillis() < time && !taskQueue.isComplete()) {
            Task task = taskQueue.next();
            if (task != null) {
                task.perform();
            }
        }
    }

    @Override
    public synchronized void clear() {
        taskQueue.clear();
    }

    @Override
    public synchronized void submit(Task task) {
        taskQueue.add(task);
    }
}
