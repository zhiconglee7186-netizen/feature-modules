package demo.tasksystem;

public class FakeTaskProcessor implements TaskProcessor {
    @Override
    public String process(TaskRecord task) throws Exception {
        Thread.sleep(2500L);
        if (task.getPayload() != null && task.getPayload().toLowerCase().contains("fail")) {
            throw new IllegalStateException("Simulated processor failure");
        }
        return "result://" + task.getTaskType() + "/" + task.getTaskId();
    }
}
