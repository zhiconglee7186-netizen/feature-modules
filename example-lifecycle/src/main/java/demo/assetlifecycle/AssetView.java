package demo.assetlifecycle;

import java.util.List;

public record AssetView(
        long assetId,
        String ownerId,
        String title,
        String sourceType,
        AssetStatus status,
        long fileSizeBytes,
        String format,
        int width,
        int height,
        List<VersionView> versions
) {
}
