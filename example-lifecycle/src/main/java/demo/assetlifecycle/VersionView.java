package demo.assetlifecycle;

public record VersionView(
        long versionId,
        VersionRole role,
        String objectKey,
        String format,
        long sizeBytes,
        int width,
        int height
) {
}
