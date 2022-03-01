package com.github.mrmks.status;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class TaskManager {

    private final LinkedList<Task> adding = new LinkedList<>();
    private Task[][] queue = new Task[32][];
    private final int[] pointer = new int[]{-1,0};

    // there will be two Task[] in cache, and if each of task reference occupied 4 bytes, there will be 512 * 2 * 4 bytes, means 4 KB;
    private final int cap = Constants.DEFAULT_ARRAY_INIT_CAPACITY;

    void addTask(Task task) {
        if (task != null) {
            adding.add(task);
        }
    }

    void tick() {

        if (!adding.isEmpty()) {
            Iterator<Task> it = adding.iterator();
            Task[] tasks;
            while (it.hasNext()) {
                if (pointer[0] < 0 || queue == null) {
                    pointer[0] = pointer[1] = 0;
                    queue = queue == null ? new Task[32][] : queue;
                    tasks = queue[0] = new Task[cap];
                } else if (pointer[1] >= cap) {
                    pointer[0] ++;
                    pointer[1] = 0;
                    if (pointer[0] >= queue.length) {
                        queue = Arrays.copyOf(queue, pointer[0] << 1);
                        tasks = queue[pointer[0]] = new Task[cap];
                    } else {
                        tasks = queue[pointer[0]];
                        if (tasks == null) tasks = queue[pointer[0]] = new Task[cap];
                    }
                } else tasks = queue[pointer[0]];

                do {
                    Task t = it.next();
                    it.remove();
                    if (!t.dealt) {
                        t.onAttach();
                        t.dealt = true;
                        tasks[pointer[1]] = t;
                        pointer[1]++;
                    }
                } while (pointer[1] < cap && it.hasNext());
            }
        }

        if ((pointer[0] ^ pointer[1]) > 0) {
            int count = 0;
            while (count <= pointer[0]) {
                Task[] tasks = queue[pointer[0]];
                for (int i = 0; i < (count == pointer[0] ? pointer[1] : tasks.length); ++i) {
                    boolean flag = tickTask(tasks[i]);
                    if (!flag) {
                        Task replace = null;
                        Task[] lastTasks;
                        do {
                            lastTasks = pointer[0] == count ? tasks : queue[pointer[0]];
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
                        if (replace == null) --pointer[1];
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

    void stopAll() {
        for (Task[] tasks : queue) {
            if (tasks != null) {
                for (Task task : tasks) {
                    if (task != null) task.cancel(true);
                }
                Arrays.fill(tasks, null);
            }
        }
        Arrays.fill(queue, null);
        queue = null;
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
