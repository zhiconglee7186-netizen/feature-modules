package demo.assetlifecycle;

import java.time.Instant;

public class AssetRecord {
    private final long assetId;
    private final String ownerId;
    private final Instant createdAt;
    private String title;
    private String sourceType;
    private AssetStatus status;
    private boolean favorite;
    private int usageCount;
    private Instant deletedAt;
    private Long fileSizeBytes;
    private String format;
    private Integer width;
    private Integer height;

    public AssetRecord(long assetId, String ownerId, String title, String sourceType) {
        this.assetId = assetId;
        this.ownerId = ownerId;
        this.title = title;
        this.sourceType = sourceType;
        this.status = AssetStatus.ACTIVE;
        this.favorite = false;
        this.usageCount = 0;
        this.createdAt = Instant.now();
    }

    public long getAssetId() {
        return assetId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
