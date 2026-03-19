package demo.tasksystem;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TaskService {
    private static final int ETA_PER_POSITION_SECONDS = 5;

    private final InMemoryTaskRepository repository;
    private final InMemoryQueueCoordinator queueCoordinator;
    private final Duration timeout;

    public TaskService(InMemoryTaskRepository repository, InMemoryQueueCoordinator queueCoordinator, Duration timeout) {
        this.repository = repository;
        this.queueCoordinator = queueCoordinator;
        this.timeout = timeout;
    }

    public TaskTicket createTask(String taskType, String payload) {
        String taskId = UUID.randomUUID().toString();
        TaskRecord record = new TaskRecord(taskId, taskType, payload);
        repository.save(record);
        queueCoordinator.enqueue(taskId, System.currentTimeMillis());
        int position = queueCoordinator.positionOf(taskId);
        return new TaskTicket(taskId, position, position * ETA_PER_POSITION_SECONDS);
    }

    public Optional<TaskRecord> findTask(String taskId) {
        return repository.findById(taskId);
    }

    public TaskSnapshot getStatus(String taskId) {
        TaskRecord task = repository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        int position = task.getStatus() == TaskStatus.QUEUED ? queueCoordinator.positionOf(taskId) : 0;
        int etaSeconds = position * ETA_PER_POSITION_SECONDS;

        return new TaskSnapshot(
                task.getTaskId(),
                task.getStatus(),
                position,
                etaSeconds,
                task.getResultRef(),
                task.getErrorMessage()
        );
    }

    public synchronized String atomicDequeue() {
        String taskId = queueCoordinator.atomicDequeue();
        if (taskId == null) {
            return null;
        }
        TaskRecord task = repository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Queued task missing from repository"));
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(Instant.now());
        repository.save(task);
        return taskId;
    }

    public boolean touchHeartbeat(String taskId) {
        return queueCoordinator.touchHeartbeat(taskId);
    }

    public synchronized void completeTask(String taskId, String resultRef) {
        TaskRecord task = repository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (task.getStatus() == TaskStatus.DONE) {
            return;
        }
        if (task.getStatus() != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task is not running: " + task.getStatus());
        }
        task.setStatus(TaskStatus.DONE);
        task.setResultRef(resultRef);
        task.setErrorMessage(null);
        task.setCompletedAt(Instant.now());
        repository.save(task);
        queueCoordinator.clearRunningIfMatches(taskId);
    }

    public synchronized void failTask(String taskId, String errorMessage) {
        TaskRecord task = repository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (task.getStatus() == TaskStatus.FAILED) {
            queueCoordinator.clearRunningIfMatches(taskId);
            return;
        }
        if (task.getStatus() != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task is not running: " + task.getStatus());
        }
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(Instant.now());
        task.setResultRef(null);
        repository.save(task);
        queueCoordinator.clearRunningIfMatches(taskId);
    }

    public synchronized void cancelTask(String taskId) {
        TaskRecord task = repository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.QUEUED) {
            queueCoordinator.removeQueued(taskId);
            task.setStatus(TaskStatus.CANCELED);
            task.setCompletedAt(Instant.now());
            repository.save(task);
            return;
        }

        if (task.getStatus() == TaskStatus.RUNNING) {
            task.setStatus(TaskStatus.CANCELED);
            task.setCompletedAt(Instant.now());
            task.setErrorMessage("Canceled during execution");
            repository.save(task);
            queueCoordinator.clearRunningIfMatches(taskId);
            return;
        }

        throw new IllegalStateException("Task cannot be canceled in state: " + task.getStatus());
    }

    public synchronized void failTimedOutRunningTask() {
        String runningTaskId = queueCoordinator.getRunningTaskId();
        if (runningTaskId == null) {
            return;
        }
        if (!queueCoordinator.isRunningTimedOut(timeout)) {
            return;
        }
        TaskRecord task = repository.findById(runningTaskId).orElse(null);
        if (task == null) {
            queueCoordinator.clearRunningIfMatches(runningTaskId);
            return;
        }
        if (task.getStatus() == TaskStatus.RUNNING) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Task timed out");
            task.setCompletedAt(Instant.now());
            repository.save(task);
        }
        queueCoordinator.clearRunningIfMatches(runningTaskId);
    }

    public String getRunningTaskId() {
        return queueCoordinator.getRunningTaskId();
    }

    public Duration getTimeout() {
        return timeout;
    }
}
