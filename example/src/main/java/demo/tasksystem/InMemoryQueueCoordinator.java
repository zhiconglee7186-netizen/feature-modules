package demo.tasksystem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryQueueCoordinator {
    private final Map<String, Long> queued = new LinkedHashMap<>();
    private String runningTaskId;
    private long heartbeatAtMillis;
    private long runningSinceMillis;

    public synchronized void enqueue(String taskId, long score) {
        queued.put(taskId, score);
    }

    public synchronized String atomicDequeue() {
        if (runningTaskId != null) {
            return null;
        }
        String firstTaskId = queued.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (firstTaskId == null) {
            return null;
        }
        queued.remove(firstTaskId);
        long now = System.currentTimeMillis();
        runningTaskId = firstTaskId;
        runningSinceMillis = now;
        heartbeatAtMillis = now;
        return firstTaskId;
    }

    public synchronized void removeQueued(String taskId) {
        queued.remove(taskId);
    }

    public synchronized int positionOf(String taskId) {
        List<String> ordered = orderedTaskIds();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).equals(taskId)) {
                return i + 1;
            }
        }
        return 0;
    }

    public synchronized List<String> orderedTaskIds() {
        return queued.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    public synchronized String getRunningTaskId() {
        return runningTaskId;
    }

    public synchronized long getHeartbeatAtMillis() {
        return heartbeatAtMillis;
    }

    public synchronized long getRunningSinceMillis() {
        return runningSinceMillis;
    }

    public synchronized boolean touchHeartbeat(String taskId) {
        if (runningTaskId == null || !runningTaskId.equals(taskId)) {
            return false;
        }
        heartbeatAtMillis = System.currentTimeMillis();
        return true;
    }

    public synchronized void clearRunningIfMatches(String taskId) {
        if (runningTaskId != null && runningTaskId.equals(taskId)) {
            runningTaskId = null;
            heartbeatAtMillis = 0L;
            runningSinceMillis = 0L;
        }
    }

    public synchronized boolean isRunningTimedOut(Duration timeout) {
        if (runningTaskId == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - heartbeatAtMillis;
        return elapsed > timeout.toMillis();
    }

    public synchronized int queuedCount() {
        return queued.size();
    }

    public synchronized List<String> snapshotQueue() {
        return new ArrayList<>(orderedTaskIds());
    }
}
