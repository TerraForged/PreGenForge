package me.dags.pregen.task;

import java.util.ArrayList;
import java.util.List;

public class TaskQueue implements Task {

    private final List<Task> tasks = new ArrayList<>();

    private int index = -1;

    @Override
    public boolean isComplete() {
        return tasks.isEmpty();
    }

    @Override
    public boolean perform() {
        Task task = next();

        if (task != null) {
            task.perform();

            // potentially more tasks to process so return true to request more processing time
            return true;
        }

        // no tasks to process so retuning false allows us to yield to other workers
        return false;
    }

    public void clear() {
        tasks.clear();
    }

    public void add(Task task) {
        tasks.add(task);
    }

    public Task next() {
        while (tasks.size() > 0) {
            if (index + 1 >= tasks.size()) {
                index = -1;
            }

            Task task = tasks.get(++index);
            if (task.isComplete()) {
                tasks.remove(index);
                continue;
            }

            return task;
        }
        return null;
    }
}
