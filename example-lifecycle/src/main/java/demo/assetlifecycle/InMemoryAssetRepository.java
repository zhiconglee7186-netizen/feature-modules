package demo.assetlifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAssetRepository {
    private final AtomicLong sequence = new AtomicLong(1000L);
    private final Map<Long, AssetRecord> store = new ConcurrentHashMap<>();

    public AssetRecord create(String ownerId, String title, String sourceType) {
        long assetId = sequence.incrementAndGet();
        AssetRecord record = new AssetRecord(assetId, ownerId, title, sourceType);
        store.put(assetId, record);
        return record;
    }

    public void save(AssetRecord record) {
        store.put(record.getAssetId(), record);
    }

    public Optional<AssetRecord> findById(long assetId) {
        return Optional.ofNullable(store.get(assetId));
    }

    public List<AssetRecord> findByOwnerId(String ownerId) {
        List<AssetRecord> results = new ArrayList<>();
        for (AssetRecord record : store.values()) {
            if (record.getOwnerId().equals(ownerId)) {
                results.add(record);
            }
        }
        results.sort(Comparator.comparing(AssetRecord::getCreatedAt));
        return results;
    }

    public void delete(long assetId) {
        store.remove(assetId);
    }
}
