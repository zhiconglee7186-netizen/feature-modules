# Sanitized Minimal Asset Lifecycle Reference

## Purpose

This document accompanies the sanitized asset lifecycle design and provides a minimal reference implementation.

The sample is intentionally generic and avoids all project-specific identifiers.

## What the Example Demonstrates

The example models a simplified but complete asset lifecycle:

1. register uploaded content as a managed asset
2. register generated output as a managed asset
3. store metadata separately from binary content
4. store multiple file versions under one logical asset
5. retrieve files through an application service with ownership checks
6. move assets to trash
7. restore assets from trash
8. permanently delete assets and release storage quota
9. avoid duplicate registration by normalized object key

## Simplifications

To keep the sample small and safe:

- all data is stored in memory
- there is no real database
- there is no real object storage vendor
- there is no real HTTP layer
- thumbnails are represented as normal versions

## Directory

The sample code lives under:

`example/src/main/java/demo/assetlifecycle`

## Main Components

- `AssetLifecycleService`
  The main orchestration layer for asset registration and lifecycle operations.

- `InMemoryAssetRepository`
  Stores logical asset metadata.

- `InMemoryAssetVersionRepository`
  Stores file-version metadata.

- `InMemoryObjectStorage`
  Stores binary file content by normalized object key.

- `InMemoryQuotaLedger`
  Tracks storage usage per owner.

- `DemoApplication`
  Demonstrates the full lifecycle flow.

## Example Flow

1. A user uploads a file.
2. The service writes the file to storage and creates a logical asset plus versions.
3. A generated result is registered.
4. The same generated result is registered again and resolves idempotently.
5. The asset is listed and inspected.
6. One asset is moved to trash and restored.
7. One asset is permanently deleted.
8. Quota usage is updated accordingly.

## How to Run

From the `example` directory:

```powershell
javac -d out src/main/java/demo/assetlifecycle/*.java
java -cp out demo.assetlifecycle.DemoApplication
```

## Why This Sample Is Useful

This reference implementation is not production-ready, but it is useful for:

- design review
- architecture explanation
- onboarding
- lifecycle walkthroughs
- showing the boundary between metadata and binary storage

It is intentionally small so the core ideas stay obvious.
