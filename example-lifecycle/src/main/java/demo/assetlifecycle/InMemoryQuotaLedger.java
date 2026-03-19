package demo.assetlifecycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryQuotaLedger {
    private final Map<String, Long> usedBytesByOwner = new ConcurrentHashMap<>();

    public void increase(String ownerId, long bytes) {
        usedBytesByOwner.merge(ownerId, bytes, Long::sum);
    }

    public void decrease(String ownerId, long bytes) {
        usedBytesByOwner.compute(ownerId, (key, current) -> {
            long next = (current == null ? 0L : current) - bytes;
            return Math.max(0L, next);
        });
    }

    public long usedBytes(String ownerId) {
        return usedBytesByOwner.getOrDefault(ownerId, 0L);
    }
}
