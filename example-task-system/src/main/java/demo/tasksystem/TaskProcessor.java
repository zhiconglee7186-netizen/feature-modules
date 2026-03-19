package demo.tasksystem;

public interface TaskProcessor {
    String process(TaskRecord task) throws Exception;
}
