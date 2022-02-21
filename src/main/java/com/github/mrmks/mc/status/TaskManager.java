package com.github.mrmks.mc.status;

import java.util.Iterator;
import java.util.LinkedList;

public class TaskManager {

    private final LinkedList<Task> adding = new LinkedList<>();
    private final LinkedList<Task[]> queue = new LinkedList<>();
    private final int[] pointer = new int[]{0,0};

    // there will be two Task[] in cache, and if each of task reference occupied 4 bytes, there will be 512 * 2 * 4 bytes, means 4 KB;
    private final int cap = Constants.DEFAULT_ARRAY_INIT_CAPACITY;

    void addTask(Task task) {
        if (task != null) {
            adding.add(task);
        }
    }

    void tick() {

        if (!adding.isEmpty()) {
            if (queue.isEmpty()) {
                queue.add(new Task[cap]);
            }
            for (Task task : adding) {
                if (!task.dealt) {
                    if (pointer[1] < cap) {
                        queue.getLast()[pointer[1]] = task;
                        pointer[1] += 1;
                    } else {
                        queue.add(new Task[cap]);
                        queue.getLast()[0] = task;
                        pointer[0] += 1;
                        pointer[1] = 0;
                    }
                    task.onAttach();
                    task.dealt = true;
                }
            }
            adding.clear();
        }

        if (!queue.isEmpty()) {
            Iterator<Task[]> it = queue.iterator();
            int count = 0;
            while (it.hasNext()) {
                Task[] tasks = it.next();
                if (count > pointer[0]) {
                    it.remove();
                    continue;
                }
                for (int i = 0; i < (count == pointer[0] ? pointer[1] : tasks.length); ++i) {
                    boolean flag = tickTask(tasks[i]);
                    if (!flag) {
                        Task replace = null;
                        Task[] lastTasks;
                        do {
                            lastTasks = pointer[0] == count ? tasks : queue.get(pointer[0]);
                            while (pointer[1] > (pointer[0] == count ? i + 1 : 0)) {
                                boolean f2 = tickTask(lastTasks[pointer[1] - 1]);
                                --pointer[1];
                                if (f2) {
                                    replace = lastTasks[pointer[1]];
                                    lastTasks[pointer[1]] = null;
                                    break;
                                } else {
                                    tasks[pointer[1]] = null;
                                }
                            }

                            if (replace != null || pointer[0] == count) break;

                            --pointer[0];
                            pointer[1] = cap;
                        } while (pointer[0] >= count);
                        tasks[i] = replace;
                    }
                }
                ++count;
            }
        }
    }

    private boolean tickTask(Task task) {
        if (task.canceled) {
            if (!task.force) {
                task.onRemove();
            }
            return false;
        } else {
            task.remaining --;
            if (task.remaining < 0) {
                task.interval = task.run(task.interval, task.reset);
                task.reset = false;
                if (task.interval < 0 || task.canceled) {
                    if (!task.force) task.onRemove();
                    return false;
                } else {
                    task.remaining = task.interval;
                }
            }
        }
        return true;
    }

    public static abstract class Task {

        private boolean dealt = false;

        private boolean canceled = false;
        private boolean force = false;
        private boolean reset = false;

        private int interval = 0;
        private int remaining = 0;

        abstract int run(int interval, boolean reset);

        void onAttach() {}
        void onRemove() {}

        void cancel(boolean force) {
            if (!canceled) {
                canceled = true;
                this.force = force;
            }
        }

        void resetRemaining() {
            interval -= remaining;
            remaining = 0;
            reset = true;
        }
    }
}
