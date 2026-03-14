package demo.tasksystem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TaskWatchdog {
    private final TaskService taskService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(daemonFactory());

    public TaskWatchdog(TaskService taskService) {
        this.taskService = taskService;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(taskService::failTimedOutRunningTask, 1L, 1L, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "task-watchdog");
            thread.setDaemon(true);
            return thread;
        };
    }
}
