package demo.tasksystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskRepository {
    private final Map<String, TaskRecord> store = new ConcurrentHashMap<>();

    public void save(TaskRecord record) {
        store.put(record.getTaskId(), record);
    }

    public Optional<TaskRecord> findById(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    public List<TaskRecord> findByStatus(TaskStatus status) {
        List<TaskRecord> results = new ArrayList<>();
        for (TaskRecord record : store.values()) {
            if (record.getStatus() == status) {
                results.add(record);
            }
        }
        results.sort(Comparator.comparing(TaskRecord::getCreatedAt));
        return results;
    }

    public List<TaskRecord> findAll() {
        List<TaskRecord> results = new ArrayList<>(store.values());
        results.sort(Comparator.comparing(TaskRecord::getCreatedAt));
        return results;
    }
}
