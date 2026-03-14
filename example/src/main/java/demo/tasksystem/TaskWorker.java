package demo.tasksystem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TaskWorker {
    private final TaskService taskService;
    private final TaskProcessor processor;
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(daemonFactory("task-poller"));
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(daemonFactory("task-heartbeat"));
    private final ScheduledExecutorService processorExecutor = Executors.newSingleThreadScheduledExecutor(daemonFactory("task-processor"));

    public TaskWorker(TaskService taskService, TaskProcessor processor) {
        this.taskService = taskService;
        this.processor = processor;
    }

    public void start() {
        poller.scheduleWithFixedDelay(this::pollOnce, 0L, 500L, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        poller.shutdownNow();
        heartbeatExecutor.shutdownNow();
        processorExecutor.shutdownNow();
    }

    private void pollOnce() {
        try {
            String taskId = taskService.atomicDequeue();
            if (taskId == null) {
                return;
            }
            processorExecutor.submit(() -> processTask(taskId));
        } catch (Exception e) {
            System.err.println("Worker poll error: " + e.getMessage());
        }
    }

    private void processTask(String taskId) {
        ScheduledFuture<?> heartbeatJob = heartbeatExecutor.scheduleWithFixedDelay(
                () -> taskService.touchHeartbeat(taskId),
                0L,
                1L,
                TimeUnit.SECONDS
        );

        try {
            TaskRecord task = taskService.findTask(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found during processing"));
            String resultRef = processor.process(task);
            taskService.completeTask(taskId, resultRef);
        } catch (Exception e) {
            try {
                taskService.failTask(taskId, e.getMessage());
            } catch (Exception nested) {
                System.err.println("Worker failure path error: " + nested.getMessage());
            }
        } finally {
            heartbeatJob.cancel(true);
        }
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
