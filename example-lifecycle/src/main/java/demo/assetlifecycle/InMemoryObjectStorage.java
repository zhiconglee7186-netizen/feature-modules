package demo.assetlifecycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObjectStorage {
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    public String normalizeObjectKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be empty");
        }
        String normalized = rawKey.trim();

        int schemeIndex = normalized.indexOf("://");
        if (schemeIndex >= 0) {
            int firstSlashAfterHost = normalized.indexOf('/', schemeIndex + 3);
            normalized = firstSlashAfterHost >= 0 ? normalized.substring(firstSlashAfterHost + 1) : "";
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Normalized object key cannot be empty");
        }
        return normalized;
    }

    public void put(String objectKey, byte[] bytes) {
        objects.put(normalizeObjectKey(objectKey), bytes.clone());
    }

    public byte[] get(String objectKey) {
        byte[] bytes = objects.get(normalizeObjectKey(objectKey));
        if (bytes == null) {
            throw new IllegalArgumentException("Binary object not found");
        }
        return bytes.clone();
    }

    public long sizeOf(String objectKey) {
        byte[] bytes = objects.get(normalizeObjectKey(objectKey));
        return bytes == null ? 0L : bytes.length;
    }

    public boolean exists(String objectKey) {
        return objects.containsKey(normalizeObjectKey(objectKey));
    }

    public void delete(String objectKey) {
        objects.remove(normalizeObjectKey(objectKey));
    }
}
