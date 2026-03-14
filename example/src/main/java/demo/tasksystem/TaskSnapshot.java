package demo.tasksystem;

public record TaskSnapshot(
        String taskId,
        TaskStatus status,
        int position,
        int etaSeconds,
        String resultRef,
        String errorMessage
) {
}
