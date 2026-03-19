package demo.assetlifecycle;

public class AssetVersionRecord {
    private final long versionId;
    private final long assetId;
    private final VersionRole role;
    private final String objectKey;
    private final String format;
    private final long sizeBytes;
    private final int width;
    private final int height;

    public AssetVersionRecord(
            long versionId,
            long assetId,
            VersionRole role,
            String objectKey,
            String format,
            long sizeBytes,
            int width,
            int height
    ) {
        this.versionId = versionId;
        this.assetId = assetId;
        this.role = role;
        this.objectKey = objectKey;
        this.format = format;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
    }

    public long getVersionId() {
        return versionId;
    }

    public long getAssetId() {
        return assetId;
    }

    public VersionRole getRole() {
        return role;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getFormat() {
        return format;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
