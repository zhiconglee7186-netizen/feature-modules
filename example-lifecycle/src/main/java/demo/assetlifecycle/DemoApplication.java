package demo.assetlifecycle;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DemoApplication {
    public static void main(String[] args) {
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        InMemoryAssetVersionRepository versionRepository = new InMemoryAssetVersionRepository();
        InMemoryObjectStorage objectStorage = new InMemoryObjectStorage();
        InMemoryQuotaLedger quotaLedger = new InMemoryQuotaLedger();

        AssetLifecycleService service = new AssetLifecycleService(
                assetRepository,
                versionRepository,
                objectStorage,
                quotaLedger
        );

        String ownerId = "owner-1";

        AssetView uploadAsset = service.registerUpload(
                ownerId,
                "Uploaded Example",
                "UPLOAD",
                "storage://public/uploads/example-main.bin",
                "uploaded-binary".getBytes(StandardCharsets.UTF_8),
                "BIN",
                800,
                600
        );

        AssetView generatedAsset = service.registerGeneratedResult(
                ownerId,
                "Generated Example",
                "result://jobs/output/generated-main.bin",
                "generated-binary".getBytes(StandardCharsets.UTF_8),
                "BIN",
                1024,
                1024
        );

        AssetView sameGeneratedAsset = service.registerGeneratedResult(
                ownerId,
                "Generated Example Duplicate",
                "result://jobs/output/generated-main.bin",
                "generated-binary".getBytes(StandardCharsets.UTF_8),
                "BIN",
                1024,
                1024
        );

        System.out.println("Uploaded asset id: " + uploadAsset.assetId());
        System.out.println("Generated asset id: " + generatedAsset.assetId());
        System.out.println("Idempotent re-registration returned asset id: " + sameGeneratedAsset.assetId());

        List<AssetView> assets = service.listAssets(ownerId);
        System.out.println("Asset count: " + assets.size());
        System.out.println("Used bytes before delete: " + service.usedBytes(ownerId));

        AssetView detail = service.getAssetDetail(ownerId, generatedAsset.assetId());
        System.out.println("Generated asset version count: " + detail.versions().size());

        long mainVersionId = detail.versions().stream()
                .filter(version -> version.role() == VersionRole.MAIN)
                .findFirst()
                .orElseThrow()
                .versionId();

        BinaryPayload payload = service.getVersionFile(ownerId, mainVersionId);
        System.out.println("Fetched object key: " + payload.objectKey());
        System.out.println("Fetched byte length: " + payload.bytes().length);

        service.moveToTrash(ownerId, uploadAsset.assetId());
        System.out.println("Upload asset moved to trash");

        service.restore(ownerId, uploadAsset.assetId());
        System.out.println("Upload asset restored");

        service.moveToTrash(ownerId, generatedAsset.assetId());
        service.permanentDelete(ownerId, generatedAsset.assetId());
        System.out.println("Generated asset permanently deleted");

        System.out.println("Used bytes after delete: " + service.usedBytes(ownerId));
        System.out.println("Remaining asset count: " + service.listAssets(ownerId).size());
    }
}
