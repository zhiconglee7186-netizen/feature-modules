package demo.tasksystem;

import java.time.Instant;

public class TaskRecord {
    private final String taskId;
    private final String taskType;
    private final String payload;
    private final Instant createdAt;
    private volatile TaskStatus status;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile String resultRef;
    private volatile String errorMessage;

    public TaskRecord(String taskId, String taskType, String payload) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.status = TaskStatus.QUEUED;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getResultRef() {
        return resultRef;
    }

    public void setResultRef(String resultRef) {
        this.resultRef = resultRef;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
