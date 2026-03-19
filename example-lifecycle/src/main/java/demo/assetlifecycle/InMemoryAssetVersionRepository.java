package demo.assetlifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAssetVersionRepository {
    private final AtomicLong sequence = new AtomicLong(5000L);
    private final Map<Long, AssetVersionRecord> store = new ConcurrentHashMap<>();

    public AssetVersionRecord create(
            long assetId,
            VersionRole role,
            String objectKey,
            String format,
            long sizeBytes,
            int width,
            int height
    ) {
        long versionId = sequence.incrementAndGet();
        AssetVersionRecord record = new AssetVersionRecord(
                versionId,
                assetId,
                role,
                objectKey,
                format,
                sizeBytes,
                width,
                height
        );
        store.put(versionId, record);
        return record;
    }

    public Optional<AssetVersionRecord> findById(long versionId) {
        return Optional.ofNullable(store.get(versionId));
    }

    public List<AssetVersionRecord> findByAssetId(long assetId) {
        List<AssetVersionRecord> results = new ArrayList<>();
        for (AssetVersionRecord record : store.values()) {
            if (record.getAssetId() == assetId) {
                results.add(record);
            }
        }
        results.sort(Comparator.comparing(AssetVersionRecord::getVersionId));
        return results;
    }

    public Optional<AssetVersionRecord> findByObjectKeyAndOwner(String objectKey, String ownerId, InMemoryAssetRepository assetRepository) {
        for (AssetVersionRecord version : store.values()) {
            if (!version.getObjectKey().equals(objectKey)) {
                continue;
            }
            AssetRecord asset = assetRepository.findById(version.getAssetId()).orElse(null);
            if (asset != null && asset.getOwnerId().equals(ownerId)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    public void deleteByAssetId(long assetId) {
        List<Long> versionIds = new ArrayList<>();
        for (Map.Entry<Long, AssetVersionRecord> entry : store.entrySet()) {
            if (entry.getValue().getAssetId() == assetId) {
                versionIds.add(entry.getKey());
            }
        }
        for (Long versionId : versionIds) {
            store.remove(versionId);
        }
    }
}
