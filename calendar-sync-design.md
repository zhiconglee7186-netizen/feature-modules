# Sanitized Calendar Sync Module
## Document Information

- **Author:** Zhicong Lee
- **Version:** v1.0
- **Last Updated:** 2026-03-11

## Purpose

This document explains the design of a multi-provider calendar synchronization module.

The module is designed to pull calendar changes from multiple external providers and normalize them into one internal event model. The key design goal is to avoid expensive full polling whenever a provider offers a better change signal such as webhooks, subscriptions, or incremental tokens.

## Design Goals

- Support multiple provider types behind one internal synchronization flow.
- Normalize all external events into one internal event store.
- Prefer incremental synchronization over full refresh.
- Keep authorization callbacks fast by moving heavy work to asynchronous bootstrap steps.
- Handle bursty webhook traffic safely with merge windows and deduplication.
- Add repair paths such as scheduled refresh and token reset fallback.
- Keep credentials protected and never log or embed secrets in source code.

## High-Level Architecture

The module is split into five responsibilities:

1. Credential management
2. Provider-specific change detection
3. Incremental synchronization
4. Internal event persistence
5. Recovery and repair tasks

### 1. Credential Management

Each provider needs a valid credential before any sync can happen. The module treats credential handling as a separate concern:

- Short-lived access tokens are cached and refreshed when needed.
- Long-lived credentials are stored in encrypted form.
- Provider-specific authorization data is never mixed with event persistence logic.

This separation keeps the sync pipeline simpler and reduces accidental leakage risk.

### 2. Provider-Specific Change Detection

The module supports three generic provider styles:

- Push provider: sends webhooks when event data changes.
- Subscription provider: requires explicit subscription creation and renewal, then sends notifications.
- CalDAV-like provider: exposes collection discovery and incremental sync tokens rather than webhooks.

The orchestrator does not care which provider produced the change. It only needs a normalized sync request such as:

- user identifier
- provider type
- remote calendar identifier
- optional incremental token

### 3. Incremental Synchronization

The sync strategy depends on provider capabilities:

- If the provider supports a sync token or delta token, the module uses it.
- If the token is missing, expired, or rejected, the module falls back to a bounded full sync.
- If the provider only sends notifications, the notification becomes a trigger and the actual data is fetched from the provider API afterward.

This makes the system resilient to token invalidation while still staying efficient in the normal path.

### 4. Internal Event Persistence

All external events are mapped into one internal model. The normalized event usually contains:

- internal user id
- provider type
- remote calendar id
- remote event id
- title
- start time
- end time
- location
- description
- status
- source metadata

The persistence layer applies upsert semantics:

- insert if the event does not exist
- update if the remote event already exists
- delete or mark deleted if the provider signals a removal

This gives downstream modules one stable data source regardless of provider differences.

### 5. Recovery and Repair Tasks

Real-world sync systems drift over time, so the module includes repair mechanisms:

- renew expiring watches or subscriptions
- retry failed asynchronous tasks
- reset invalid incremental tokens
- periodically reconcile providers that do not offer strong push guarantees

Without these repair paths, a calendar sync system may appear correct at first but degrade after days or weeks in production.

## Main Flows

### Flow A: Authorization Success Bootstrap

After a user authorizes a provider, the module should not block the callback request with heavy sync work. Instead it should:

1. Persist or update the authorization record.
2. Dispatch an asynchronous bootstrap task.
3. Load the remote calendar list.
4. Perform the first bounded full sync.
5. Register watches or subscriptions if the provider supports them.
6. Save the initial incremental token if one is returned.

This pattern reduces callback latency and improves reliability.

### Flow B: Webhook or Notification Handling

Push-style providers can emit many notifications in a short time. A naive implementation will over-sync the same calendar repeatedly.

The safer pattern is:

1. Build a lock key from user id and calendar id.
2. Try to acquire a short-lived lock.
3. If the lock already exists, set a pending flag and return quickly.
4. If the lock is acquired, trigger one sync job.
5. Let the lock expire naturally.
6. If another notification arrives after the window, the pending flag causes one more sync.

This is a merge-window strategy. It trades a small delay for much better backend stability.

### Flow C: Incremental Sync

The incremental sync path usually follows this logic:

1. Load the current incremental token.
2. Call the provider delta endpoint.
3. Upsert changed events.
4. Remove deleted events.
5. Save the next token.
6. If the provider rejects the token, clear it and schedule a bounded full sync.

This pattern is the main reason the module scales.

### Flow D: CalDAV-Like Provider Sync

CalDAV-style providers often require extra protocol steps:

1. Discover the principal URL.
2. Discover the calendar home URL.
3. List calendars.
4. Run full collection fetch on first sync.
5. Run sync-collection with a stored token on later syncs.
6. If the response omits event payloads, fetch changed resources by href in a second request.
7. Parse ICS payloads into normalized events.

This path is harder to implement than webhook-based providers because the server speaks protocol primitives instead of high-level JSON resources.

## Why the Module Is Structured This Way

### Separation of concerns

Provider adapters handle remote protocol details. The orchestrator handles sync flow. Repositories handle state. This keeps each piece testable.

### Async bootstrap

A slow authorization callback creates a bad user experience and a fragile control path. Offloading initial sync and watch registration removes that bottleneck.

### Merge-window webhook handling

Webhook storms are common. Deduplication at the trigger layer is much cheaper than repeated downstream sync jobs.

### Incremental first, full sync as fallback

This gives a practical balance between efficiency and correctness. Delta paths keep the steady state cheap, while fallback paths protect correctness after token loss or provider-side resets.

### Normalized local storage

Other application features should not need to understand provider-specific event formats. A unified local model is the contract boundary.


## Minimal Reference Implementation

A minimal sanitized Java example for this design is included next to this document. The sample demonstrates:

- provider abstraction
- async bootstrap
- webhook merge-window coordination
- incremental token storage
- normalized event persistence


