# Sanitized Asynchronous Task System Design

## Purpose

This document explains the design of a generic asynchronous task processing module in a fully sanitized way.

It intentionally avoids all project-specific identifiers, including:

- project names
- repository names
- domain names
- IP addresses
- API paths
- database names
- queue keys
- class names
- vendor-specific configuration values
- credentials and secrets

The goal is to preserve the engineering ideas without exposing implementation details that could leak private information.

## Problem the Module Solves

Some application actions are too slow or too failure-prone to execute directly inside a user request. Typical examples include:

- media processing
- AI inference
- document transformation
- multi-step external workflow execution
- long-running batch jobs

For this kind of work, a request-response model is not enough. The system needs a task module that can:

- accept jobs quickly
- queue them safely
- process them asynchronously
- report progress and status
- recover from worker crashes
- persist final results

## Design Goals

- Keep task creation fast and lightweight.
- Ensure only the allowed number of workers process tasks at the same time.
- Separate queue state from durable business state.
- Detect and clean up stuck tasks automatically.
- Make task completion idempotent where possible.
- Provide user-facing status queries without exposing internal execution details.
- Keep result persistence part of the same operational flow.

## Core Design Idea

The module splits task handling into four cooperating layers:

1. Durable task state in a relational database
2. Fast queue and lock coordination in a key-value store
3. A background worker that executes tasks
4. A watchdog that repairs abnormal states

This separation gives a practical balance:

- the database is the source of truth for task lifecycle
- the cache or queue store is optimized for scheduling and locking
- the worker focuses on execution
- the watchdog handles failure recovery

## High-Level Architecture

### 1. Task Creation Layer

When a client submits a task, the system:

1. generates a unique task id
2. writes a task record to the database with status `QUEUED`
3. pushes the task id into an ordered queue
4. returns task metadata such as queue position and estimated wait time

This design is useful because the user request finishes quickly while the heavy work moves to the background.

### 2. Queue Coordination Layer

The queue layer is responsible for:

- ordered enqueue
- atomic dequeue
- preventing two workers from claiming the same task
- tracking the currently running task
- storing heartbeat timestamps

A common design pattern is:

- one sorted queue for waiting tasks
- one running lock for the active task
- one heartbeat field for liveness

The important point is not the exact data structure, but that dequeue and lock acquisition happen atomically.

### 3. Worker Execution Layer

A background agent polls the queue periodically. When it successfully claims a task, it:

1. marks the task as `RUNNING`
2. starts periodic heartbeat updates
3. loads task parameters
4. dispatches execution based on task type
5. stores the result
6. marks the task `DONE` or `FAILED`
7. releases the running lock

This worker acts like a small scheduler plus executor.

### 4. Watchdog Layer

The watchdog is a repair mechanism. It periodically scans for tasks that appear stuck because:

- the worker crashed
- the process was restarted
- the lock remained but execution stopped
- the database and queue store drifted out of sync

The watchdog typically:

- checks for expired heartbeats
- compares running-lock state with database state
- marks timed-out tasks as failed
- clears stale running locks
- optionally requeues tasks in special cases

Without this layer, one bad task can block the whole pipeline.

## State Model

The module usually has a small task state machine:

- `QUEUED`
- `RUNNING`
- `DONE`
- `FAILED`
- `CANCELED`

Recommended transitions:

- `QUEUED -> RUNNING`
- `RUNNING -> DONE`
- `RUNNING -> FAILED`
- `QUEUED -> CANCELED`
- `RUNNING -> CANCELED` if cooperative cancellation is supported

Good task systems keep this state machine explicit and conservative.

## Why Use Both Database and Queue Store

Using only a database makes scheduling slower and locking harder.

Using only an in-memory queue makes recovery and auditability weak.

Using both gives clear benefits:

- database for durable lifecycle records
- queue store for fast ordering and locking
- easy status queries
- better crash recovery

This is one of the most important design decisions in the module.

## Atomic Dequeue and Single-Worker Control

One of the trickiest parts is preventing duplicate execution.

The safe pattern is:

1. check whether a running lock already exists
2. if a task is already running, do not dequeue another one
3. otherwise remove the earliest queued task
4. create the running lock in the same atomic step

If this is not atomic, two workers may claim the same task under concurrency.

That is why many implementations use a small server-side script or transaction inside the queue store instead of multiple separate commands.

## Heartbeat Design

Long-running tasks need liveness tracking.

The worker updates a heartbeat timestamp periodically while processing. The watchdog compares that timestamp against a timeout threshold.

This is better than relying only on the task start time because:

- some tasks run for a long time but are healthy
- a live heartbeat proves forward progress
- a stale heartbeat indicates the worker is gone or hung

Heartbeat turns task supervision from guesswork into a measurable rule.

## Timeout and Recovery Strategy

A robust timeout strategy usually follows this order:

1. determine whether the task is still alive from heartbeat
2. if heartbeat is stale, treat the task as abnormal
3. mark the task failed with a timeout reason
4. release the running lock
5. let later tasks continue

This prevents queue starvation.

Some systems requeue timed-out tasks automatically, but that should be used carefully because repeated retries can amplify a bad task definition or a broken external dependency.

## Task Type Dispatch

The worker normally supports multiple task types behind one scheduler.

A common structure is:

- task creation stores a `taskType`
- the worker switches on that type
- each type has its own adapter or handler

This keeps queue logic generic while allowing specialized execution paths.

For example, different task types may:

- call different external engines
- use different parameter parsing logic
- produce different result payloads
- need different fallback behavior

That separation is a good sign of maintainable design.

## External Engine Orchestration

In many real systems, the worker is not doing the heavy computation itself. It is orchestrating an external processing engine.

The execution flow often looks like this:

1. upload or prepare input data
2. inject task parameters into a workflow definition
3. start remote execution
4. poll remote history or status
5. download the final output
6. persist result metadata

The design challenge here is reliability, not just API calling.

You need to handle:

- empty outputs
- validation failures
- engine-side timeouts
- partial success
- retry safety
- result normalization

This orchestration layer is often where the most engineering complexity lives.

## Result Persistence

A good task system does not stop at “worker got a result”.

It also needs to:

- save output metadata durably
- associate output with the requesting user or entity
- register file versions if versioning exists
- persist dimensions, size, and format if relevant
- make the output queryable by later modules

This turns raw execution into a reusable product capability.

In stronger designs, result persistence is treated as part of task completion, not as an unrelated afterthought.

## Idempotency Considerations

A task module should assume that some completion steps may happen twice due to:

- retries
- worker restarts
- network uncertainty
- race conditions between repair logic and normal completion

Useful idempotent patterns include:

- checking whether a result is already registered before inserting again
- comparing normalized output identifiers
- guarding completion updates by current task state

Idempotency is especially important near task completion because that is where duplicate side effects are most expensive.

## Cancellation Design

Cancellation usually needs two modes:

- queued cancellation: remove the task from the queue and mark it canceled
- running cancellation: set a cancel flag and let the worker stop cooperatively

Queued cancellation is easy.

Running cancellation is harder because it depends on whether the external engine can be interrupted safely. If not, cancellation may only mean “ignore the final result and mark the task canceled”.

## Status Query Design

Client-facing status queries should not expose low-level engine internals.

A clean response usually contains:

- task id
- status
- queue position if still waiting
- estimated wait time if available
- result URL or result reference if completed
- error message if failed

This gives the client enough information while keeping the backend free to evolve.

## Estimated Wait Time

Many systems return a simple ETA based on queue position and an average per-task duration.

This ETA is approximate, but still useful because it:

- improves user feedback
- reduces repeated refresh behavior
- makes the async model feel predictable

It should be presented as an estimate, not a guarantee.

## Operational Strengths of This Design

This module design has several strong engineering properties:

- It decouples user requests from heavy execution.
- It reduces duplicate work through atomic claim logic.
- It survives worker failure better than a naive background thread approach.
- It provides both fast queue coordination and durable lifecycle history.
- It supports multiple task categories behind one scheduler.
- It gives operators a clear place to add repair logic.

## Tradeoffs and Weaknesses

This design also has real tradeoffs:

- More moving parts than a simple synchronous API
- Need to keep database state and queue state consistent
- Need careful timeout tuning
- Single-running-task designs limit throughput unless extended to multiple workers or multiple queues
- Watchdog logic can become complex if retries and requeue rules are added

So this pattern is powerful, but it should be used when the workload truly justifies it.

## Recommended Extensions

If this module needs to grow, the usual extension directions are:

- multi-queue support by task class or priority
- multiple workers with partitioned running locks
- retry policies with capped attempts
- dead-letter queue for permanent failures
- progress reporting from external engines
- observability metrics for queue depth, execution time, timeout rate, and success rate

These features are natural evolutions of the current architecture.

## Sanitization Notes

This document is intentionally abstracted. It does not reveal:

- any private infrastructure layout
- any proprietary workflow schema
- any storage path convention
- any internal table naming
- any business-sensitive task taxonomy
- any deployment or customer information

It is suitable for architectural discussion, onboarding, and design review without disclosing implementation-sensitive details.
