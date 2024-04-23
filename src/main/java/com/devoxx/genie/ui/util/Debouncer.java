package com.devoxx.genie.ui.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Debouncer<T> {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>();
    private final Callback<T> callback;
    private final int interval;

    /**
     * Creates a new Debouncer instance with the specified callback and interval.
     *
     * @param callback the callback function to be invoked when debouncing is triggered
     * @param interval the interval in milliseconds between debouncing triggers
     *                 (the time period in which the callback will not be invoked)
     */
    public Debouncer(Callback<T> callback, int interval) {
        this.callback = callback;
        this.interval = interval;
    }

    /**
     * Adds a new task to be executed after a debounced interval for the given key.
     * If a task for the key already exists, it will be extended if possible.
     * If a new task is added or an existing task is successfully extended, the task will be scheduled for execution.
     *
     * @param key the key associated with the task
     */
    public void call(T key) {
        TimerTask task = new TimerTask(key);

        TimerTask prev;
        do {
            prev = delayedMap.putIfAbsent(key, task);
            if (prev == null) {
                scheduler.schedule(task, interval, TimeUnit.MILLISECONDS);
            }
        }
        while (prev != null && !prev.extend()); // Exit only if a new task was added to map, or existing task was extended successfully
    }

    /**
     * Terminates the Debounce by shutting down the scheduler.
     */
    public void terminate() {
        scheduler.shutdownNow();
    }

    /**
     * Clears the delayedMap, removing all tasks scheduled for execution.
     */
    public void clear() {
        delayedMap.clear();
    }

    /**
     * A private nested class that represents a time-based task.
     */
    private class TimerTask implements Runnable {
        private final T key;
        private long dueTime;
        private final Object lock = new Object();

        public TimerTask(T key) {
            this.key = key;
            extend();
        }

        public boolean extend() {
            synchronized (lock) {
                if (dueTime < 0) {
                    return false;
                } else {
                    dueTime = System.currentTimeMillis() + interval;
                    return true;
                }
            }
        }

        @Override
        public void run() {
            synchronized (lock) {
                long remainingTime = dueTime - System.currentTimeMillis();
                if (remainingTime > 0 && delayedMap.containsKey(key)) {
                    scheduler.schedule(this, remainingTime, TimeUnit.MILLISECONDS);
                } else {
                    dueTime = -1;
                    try {
                        callback.call(key);
                    } finally {
                        delayedMap.remove(key);
                    }
                }
            }
        }
    }

    /**
     * A Callback interface that defines the method call(T t) to be implemented by the caller.
     * This interface is used to provide a callback function to be invoked when debouncing is triggered.
     *
     * @param <T> the type of the parameter passed to the callback method
     */
    public interface Callback<T> {
        public void call(T t);
    }

}
