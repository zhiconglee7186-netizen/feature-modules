# Sanitized Asset Lifecycle Design

## Purpose

This document explains the design of a generic asset lifecycle module .

It is written to preserve the engineering ideas.


## What “Asset Lifecycle” Means

In this document, an asset is any persisted output that can later be browsed, downloaded, versioned, restored, or deleted.

Typical examples include:

- uploaded source files
- generated outputs
- transformed derivatives
- thumbnails
- alternate versions

The core idea of the module is that processing does not end when a file is created. The result must enter a managed lifecycle so that the rest of the application can use it reliably.

## Design Goals

- Persist generated or uploaded outputs as first-class assets.
- Support versioned files rather than a single unstructured blob.
- Separate metadata storage from binary storage.
- Keep file retrieval stable even if object keys evolve.
- enforce per-user ownership and access control
- support trash, restore, and permanent deletion flows
- keep storage usage and quota accounting consistent
- provide repair paths for historical or partially migrated data
- make result registration idempotent where possible

## High-Level Architecture

The asset lifecycle module is usually split into five responsibilities:

1. asset metadata model
2. binary storage layer
3. asset registration flow
4. asset retrieval and authorization
5. lifecycle operations and repair jobs

## 1. Asset Metadata Model

The metadata layer gives structure to all persisted outputs.

A common model uses two levels:

- asset
- asset version

The asset record represents the logical item visible to the user. The version record represents one stored file belonging to that item.

This is useful because one logical asset may contain:

- a main file
- a thumbnail
- the original upload
- one or more derived outputs

Without this separation, storage becomes hard to query and future changes become painful.

## Suggested Logical Fields

At the asset level, the model often stores:

- asset id
- owner id
- title
- source type
- producing tool or workflow category
- status
- favorite flag
- usage count
- created time
- deleted time

At the version level, the model often stores:

- version id
- asset id
- version role
- object key
- format
- width
- height
- size in bytes

The exact names are not important. The separation of logical asset and physical file version is the important design choice.

## 2. Binary Storage Layer

The binary storage layer is responsible for persisting file content in object storage or a file store.

Its duties usually include:

- upload file content
- retrieve file streams
- delete stored objects
- inspect object metadata such as file size
- generate stable access URLs

The design should avoid coupling object storage details directly into higher-level business code.

That means application services should not need to know:

- which storage vendor is used
- which bucket layout exists
- how URLs are signed or routed

They should work with a storage abstraction instead.

## Why Metadata and Binary Storage Should Be Separate

This separation gives several benefits:

- metadata queries remain fast and relational
- binary storage can scale independently
- object keys can change without rewriting application logic
- access control stays at the application layer instead of being scattered through storage logic

This is one of the core architectural strengths of the module.

## 3. Asset Registration Flow

The most important path is how a newly produced result becomes a managed asset.

The generalized flow looks like this:

1. obtain the output file or output reference
2. normalize its file format and metadata
3. read or infer dimensions and size
4. store the binary in object storage if not already stored there
5. create or reuse an asset record
6. create the needed version records
7. expose a stable application-facing file URL

This turns raw output into a lifecycle-managed resource.

## Registration Sources

Assets may originate from more than one source:

- direct user uploads
- asynchronous processing results
- imported historical files
- migrated records from an older system

A good design does not assume only one ingestion path.

## Idempotent Result Registration

One of the hardest practical issues is duplicate registration.

Duplicates can happen because of:

- worker retries
- network uncertainty
- delayed completion callbacks
- recovery jobs
- backfill operations

The module should therefore try to make registration idempotent.

Common patterns include:

- check whether the same normalized storage key already exists
- check whether a version already points to the same binary object
- guard repeated inserts by task id or result identity

This is especially important for asynchronous processing pipelines.

## 4. Multi-Source File Acquisition

A strong asset registration flow does not assume that every result arrives in one format.

In practice, the module may need to read output from several sources:

- a local temporary directory
- a generated application URL
- an external engine URL
- an object storage location

The design should therefore support fallback acquisition:

1. try the cheapest and most direct source first
2. if unavailable, try a secondary source
3. if still unavailable, fail explicitly rather than registering broken metadata

This makes the system much more resilient to infrastructure drift.

## 5. Version Roles

Version roles are a useful abstraction because not all files attached to an asset serve the same purpose.

Common roles include:

- main
- thumbnail
- original
- derived

Role-based design allows the rest of the application to ask clear questions such as:

- what should be shown in the asset list
- what should be downloaded by default
- what should be used as the preview image

This is much cleaner than guessing based on file names.

## 6. Stable Retrieval Design

Users and downstream modules need a stable way to access files.

A robust design avoids exposing raw object storage paths directly. Instead, it uses an application-facing file endpoint or file reference.

That endpoint typically:

1. loads the version metadata
2. verifies ownership or access rights
3. resolves the object key
4. reads the binary from storage
5. streams it back to the caller

This gives the system freedom to change internal storage layout later without breaking callers.

## 7. Ownership and Access Control

Asset access control should be enforced at the application layer, not assumed from the storage backend.

The retrieval flow normally checks:

- whether the version exists
- whether the parent asset exists
- whether the caller owns or may access the asset

Only after those checks should the file stream be returned.

This design keeps authorization consistent across:

- asset list queries
- detail pages
- direct file downloads
- thumbnail loads

## 8. Object Key Normalization

Real systems often accumulate historical inconsistencies in stored file references.

Examples include:

- full URLs stored where relative keys were expected
- legacy path prefixes
- keys copied from another subsystem
- partially migrated records

To stay robust, the module benefits from a normalization step before reading or deleting storage objects.

That means:

- strip URL host information if present
- extract the underlying object path
- repair old path variants into the current storage form
- reject empty or invalid keys early

Normalization is not glamorous, but it is a major source of production reliability.

## 9. Asset Listing and Detail Views

The user-facing asset experience usually needs two different read models:

- list view
- detail view

The list view should be cheap and summary-oriented:

- title
- preview URL
- status
- source type
- dimensions
- created time

The detail view can include:

- all versions
- format and size
- main file URL
- thumbnail URL
- metadata useful for download or history

Separating these read models keeps the browsing path fast.

## 10. Trash and Restore Design

A strong lifecycle design usually does not hard-delete immediately.

Instead, deletion is split into:

- soft delete to trash
- restore from trash
- permanent delete

Soft delete is useful because it:

- protects users from accidental loss
- preserves auditability for a time
- makes batch cleanup safer

Restore should simply reverse the soft-delete markers if the asset is still recoverable.

## 11. Permanent Deletion Design

Permanent deletion is more complex than soft deletion because it must clean up both metadata and stored binaries.

A careful flow is:

1. verify the asset is already in trash
2. collect all live versions
3. deduplicate storage objects if multiple versions refer to the same binary
4. delete stored objects
5. compute released storage usage
6. delete version metadata
7. delete the asset record
8. update quota usage

The deduplication step matters because different logical versions may point to the same physical object.

Without that, the system can:

- try to delete the same object multiple times
- subtract storage usage more than once

## 12. Storage Quota Accounting

Storage usage is not just a display concern. It is part of lifecycle correctness.

The module should update usage when:

- a new binary object is registered
- an object is permanently deleted
- historical assets are repaired or migrated

Quota accounting should ideally be based on real binary occupancy rather than naive record count.

This becomes especially important when thumbnails reuse the same underlying object or when multiple versions reference the same file.

## 13. Historical Repair and Backfill

One of the most valuable parts of a mature asset lifecycle module is the existence of repair paths.

Real-world cases include:

- old task results that finished successfully but were never registered as assets
- legacy file references stored in outdated formats
- missing thumbnails
- metadata rows that exist while binaries are missing
- binaries that exist while metadata is missing

The module should support controlled repair jobs such as:

- backfill historical outputs into the asset store
- normalize old object references
- verify storage consistency
- rebuild missing metadata

This is often what separates a demo system from an operational one.

## 14. Interaction with Asynchronous Processing

The asset lifecycle module usually sits at the end of an asynchronous task pipeline.

The task system produces a result, but the asset module turns it into a usable product artifact.

That means task completion is not truly complete until:

- the output is stored
- the asset is registered
- the result is queryable

This is why the asset lifecycle should be treated as part of the execution closure, not just a UI concern.

## 15. Failure Handling Strategy

The module should fail loudly when it cannot guarantee lifecycle integrity.

Useful failure rules include:

- do not create metadata for an unreadable output
- do not return success if storage upload failed
- do not silently skip access-control checks
- keep the error reason attached to the calling workflow when possible

It is better to surface a controlled failure than to create a broken asset that later causes hidden downstream bugs.

## 16. Why This Design Is Strong

This design has several practical strengths:

- outputs become durable product objects instead of disposable files
- versioning gives room for future growth
- storage and metadata concerns stay separated
- retrieval remains stable even if internal storage changes
- deletion and quota accounting are explicit
- historical repair is supported rather than ignored

These are signs of a real lifecycle design rather than a one-off file upload feature.

## 17. Tradeoffs

This module also introduces real complexity:

- more tables or records than a single-file model
- more lifecycle states to maintain
- more care needed around duplicate registration
- more operational work for repair and cleanup

That complexity is justified when generated outputs are core product entities, not disposable temporary files.

## 18. Recommended Extensions

If the module needs to evolve further, useful additions include:

- reference counting for shared binaries
- hash-based binary deduplication
- background thumbnail generation
- integrity scanners for metadata versus storage
- retention policies for trash
- lifecycle audit logs
- download access tokens or signed URL support

These can all build naturally on the same core model.

## Sanitization Notes

This document is fully generalized. It avoids any identifier that could reveal:

- the original system
- the original infrastructure
- the original storage layout
- the original business domain
- the original customer or deployment context

It is safe to use for architectural discussion, onboarding, and internal design explanation without exposing sensitive implementation details.
