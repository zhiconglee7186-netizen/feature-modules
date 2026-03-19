package demo.tasksystem;

import java.time.Duration;
import java.util.List;

public class DemoApplication {
    public static void main(String[] args) throws Exception {
        InMemoryTaskRepository repository = new InMemoryTaskRepository();
        InMemoryQueueCoordinator queueCoordinator = new InMemoryQueueCoordinator();
        TaskService taskService = new TaskService(repository, queueCoordinator, Duration.ofSeconds(4));

        TaskWorker worker = new TaskWorker(taskService, new FakeTaskProcessor());
        TaskWatchdog watchdog = new TaskWatchdog(taskService);

        worker.start();
        watchdog.start();

        List<TaskTicket> tickets = List.of(
                taskService.createTask("type-a", "first payload"),
                taskService.createTask("type-b", "second payload"),
                taskService.createTask("type-c", "please fail")
        );

        for (int i = 0; i < 12; i++) {
            System.out.println("---- snapshot " + i + " ----");
            for (TaskTicket ticket : tickets) {
                TaskSnapshot snapshot = taskService.getStatus(ticket.taskId());
                System.out.printf(
                        "%s status=%s position=%d eta=%ds result=%s error=%s%n",
                        snapshot.taskId(),
                        snapshot.status(),
                        snapshot.position(),
                        snapshot.etaSeconds(),
                        snapshot.resultRef(),
                        snapshot.errorMessage()
                );
            }
            Thread.sleep(1000L);
        }

        watchdog.stop();
        worker.stop();
        System.exit(0);
    }
}
