package demo.assetlifecycle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AssetLifecycleService {
    private final InMemoryAssetRepository assetRepository;
    private final InMemoryAssetVersionRepository versionRepository;
    private final InMemoryObjectStorage objectStorage;
    private final InMemoryQuotaLedger quotaLedger;

    public AssetLifecycleService(
            InMemoryAssetRepository assetRepository,
            InMemoryAssetVersionRepository versionRepository,
            InMemoryObjectStorage objectStorage,
            InMemoryQuotaLedger quotaLedger
    ) {
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.objectStorage = objectStorage;
        this.quotaLedger = quotaLedger;
    }

    public AssetView registerUpload(
            String ownerId,
            String title,
            String sourceType,
            String objectKey,
            byte[] bytes,
            String format,
            int width,
            int height
    ) {
        String normalizedKey = objectStorage.normalizeObjectKey(objectKey);
        objectStorage.put(normalizedKey, bytes);

        AssetRecord asset = assetRepository.create(ownerId, title, sourceType);
        asset.setFormat(format);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setFileSizeBytes((long) bytes.length);
        assetRepository.save(asset);

        versionRepository.create(asset.getAssetId(), VersionRole.MAIN, normalizedKey, format, bytes.length, width, height);
        versionRepository.create(asset.getAssetId(), VersionRole.ORIGINAL, normalizedKey, format, bytes.length, width, height);
        quotaLedger.increase(ownerId, bytes.length);

        return getAssetDetail(ownerId, asset.getAssetId());
    }

    public AssetView registerGeneratedResult(
            String ownerId,
            String title,
            String resultRef,
            byte[] bytes,
            String format,
            int width,
            int height
    ) {
        String normalizedKey = objectStorage.normalizeObjectKey(resultRef);
        Optional<AssetVersionRecord> existingVersion = versionRepository.findByObjectKeyAndOwner(normalizedKey, ownerId, assetRepository);
        if (existingVersion.isPresent()) {
            AssetVersionRecord version = existingVersion.get();
            AssetRecord asset = assetRepository.findById(version.getAssetId())
                    .orElseThrow(() -> new IllegalStateException("Existing version points to missing asset"));
            if (!"GENERATED".equals(asset.getSourceType())) {
                asset.setSourceType("GENERATED");
                assetRepository.save(asset);
            }
            return getAssetDetail(ownerId, asset.getAssetId());
        }

        objectStorage.put(normalizedKey, bytes);

        AssetRecord asset = assetRepository.create(ownerId, title, "GENERATED");
        asset.setFormat(format);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setFileSizeBytes((long) bytes.length);
        assetRepository.save(asset);

        versionRepository.create(asset.getAssetId(), VersionRole.MAIN, normalizedKey, format, bytes.length, width, height);
        versionRepository.create(
                asset.getAssetId(),
                VersionRole.THUMBNAIL,
                normalizedKey,
                format,
                bytes.length,
                width,
                height
        );
        quotaLedger.increase(ownerId, bytes.length);

        return getAssetDetail(ownerId, asset.getAssetId());
    }

    public List<AssetView> listAssets(String ownerId) {
        List<AssetView> views = new ArrayList<>();
        for (AssetRecord asset : assetRepository.findByOwnerId(ownerId)) {
            views.add(toView(asset));
        }
        return views;
    }

    public AssetView getAssetDetail(String ownerId, long assetId) {
        AssetRecord asset = requireOwnedAsset(ownerId, assetId);
        asset.incrementUsageCount();
        assetRepository.save(asset);
        return toView(asset);
    }

    public BinaryPayload getVersionFile(String ownerId, long versionId) {
        AssetVersionRecord version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));
        AssetRecord asset = requireOwnedAsset(ownerId, version.getAssetId());
        if (asset.getStatus() == AssetStatus.DELETED) {
            throw new IllegalStateException("Asset is in trash");
        }
        return new BinaryPayload(version.getObjectKey(), objectStorage.get(version.getObjectKey()));
    }

    public void moveToTrash(String ownerId, long assetId) {
        AssetRecord asset = requireOwnedAsset(ownerId, assetId);
        asset.setStatus(AssetStatus.DELETED);
        asset.setDeletedAt(Instant.now());
        assetRepository.save(asset);
    }

    public void restore(String ownerId, long assetId) {
        AssetRecord asset = requireOwnedAsset(ownerId, assetId);
        if (asset.getStatus() != AssetStatus.DELETED) {
            throw new IllegalStateException("Asset is not in trash");
        }
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setDeletedAt(null);
        assetRepository.save(asset);
    }

    public void permanentDelete(String ownerId, long assetId) {
        AssetRecord asset = requireOwnedAsset(ownerId, assetId);
        if (asset.getStatus() != AssetStatus.DELETED) {
            throw new IllegalStateException("Asset must be in trash before permanent deletion");
        }

        List<AssetVersionRecord> versions = versionRepository.findByAssetId(assetId);
        Set<String> uniqueObjectKeys = new HashSet<>();
        long releasedBytes = 0L;

        for (AssetVersionRecord version : versions) {
            if (uniqueObjectKeys.add(version.getObjectKey())) {
                releasedBytes += objectStorage.sizeOf(version.getObjectKey());
                objectStorage.delete(version.getObjectKey());
            }
        }

        versionRepository.deleteByAssetId(assetId);
        assetRepository.delete(assetId);
        quotaLedger.decrease(ownerId, releasedBytes);
    }

    public long usedBytes(String ownerId) {
        return quotaLedger.usedBytes(ownerId);
    }

    private AssetRecord requireOwnedAsset(String ownerId, long assetId) {
        AssetRecord asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        if (!asset.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied");
        }
        return asset;
    }

    private AssetView toView(AssetRecord asset) {
        List<VersionView> versions = versionRepository.findByAssetId(asset.getAssetId()).stream()
                .map(version -> new VersionView(
                        version.getVersionId(),
                        version.getRole(),
                        version.getObjectKey(),
                        version.getFormat(),
                        version.getSizeBytes(),
                        version.getWidth(),
                        version.getHeight()
                ))
                .toList();

        return new AssetView(
                asset.getAssetId(),
                asset.getOwnerId(),
                asset.getTitle(),
                asset.getSourceType(),
                asset.getStatus(),
                asset.getFileSizeBytes() == null ? 0L : asset.getFileSizeBytes(),
                asset.getFormat(),
                asset.getWidth() == null ? 0 : asset.getWidth(),
                asset.getHeight() == null ? 0 : asset.getHeight(),
                versions
        );
    }
}
