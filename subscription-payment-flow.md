# Subscription and Payment Flow
## Document Information

- **Author:** Zhicong Lee
- **Version:** v1.0.4
- **Last Updated:** 2026-04-18

## Scope

This document explains the backend design and execution flow of the subscription and payment module.

It is intentionally sanitized:

- No environment-specific URLs
- No secrets or webhook signing material
- No provider account identifiers
- No plan configuration values tied to deployment
- No infrastructure parameters that could leak operational details

The goal is to describe the system behavior and design decisions, not deployment data.

## What This Module Does

The module manages recurring subscriptions for application users and connects payment provider events to local account state.

At a high level, it supports:

- Listing sellable subscription plans
- Starting a checkout flow through a payment provider
- Reading the current subscription state of the logged-in user
- Canceling automatic renewal
- Resuming automatic renewal
- Receiving asynchronous payment provider webhooks
- Persisting subscription and payment records locally
- Synchronizing local state with account and device access rules

## Design Overview

The design separates responsibilities into five layers.

### 1. Public API Layer

The controller layer exposes user-facing subscription APIs and payment webhook endpoints.

Main responsibilities:

- Receive authenticated subscription requests
- Validate checkout input
- Return plan and subscription view models
- Receive provider webhooks without JWT authentication

### 2. Subscription Orchestration Layer

The subscription service is the orchestration layer for business rules.

Main responsibilities:

- Decide whether the user is allowed to subscribe
- Prevent duplicate paid subscriptions
- Enforce prerequisite checks such as device binding
- Select the requested payment provider
- Build a normalized response for the frontend
- Support cancel and resume actions for recurring billing

### 3. Payment Provider Abstraction Layer

The system uses a provider interface so that payment channels can be integrated behind one contract.

Main responsibilities:

- Expose whether a provider is enabled
- Declare the provider channel name
- Create a checkout session for recurring subscriptions

This keeps provider-specific code isolated from the main business flow.

### 4. Webhook Processing Layer

Webhook handling is the authoritative backend entry for payment success and subscription lifecycle changes.

Main responsibilities:

- Verify provider signatures
- Parse provider events
- Map external events to local subscription state
- Persist payment records
- Update account-level and device-level access

### 5. Persistence Layer

The persistence layer stores the local source of truth needed by the application.

Core entities:

- `SubscriptionPlan`: sellable plan definitions
- `UserSubscription`: the user’s current subscription lifecycle state
- `Payment`: payment history for initial and renewal charges

## Core Data Model

### SubscriptionPlan

This represents the product catalog exposed by the backend.

Typical responsibilities:

- Business plan identifier
- Display name
- Billing amount and currency
- Billing interval
- Mapping to provider-side recurring billing objects
- Active/inactive visibility

### UserSubscription

This is the central record used by the application to decide whether the account is currently entitled to premium access.

Typical responsibilities:

- Which user owns the subscription
- Which business plan is active
- Which payment channel created it
- External provider subscription reference
- Local lifecycle status
- Current period boundaries
- Whether renewal is scheduled to stop at period end

### Payment

This records successful recurring charges and serves as billing history.

Typical responsibilities:

- User reference
- Subscription reference
- Plan identifier
- Payment channel
- External payment reference
- Amount and currency
- Payment status
- Paid timestamp

## End-to-End Flow

## 1. Plan Listing

The frontend first requests the list of available plans.

Backend behavior:

- Load active plans from local storage
- Check which payment providers are currently enabled
- Build a frontend-facing plan list
- Mark whether each plan is available through each provider

Design intent:

- The pricing page should not hardcode payment-channel availability
- Product visibility and channel capability remain backend-controlled

## 2. Checkout Creation

When a user selects a paid plan, the frontend sends a checkout request containing:

- The business plan identifier
- The selected payment channel
- Optional success/cancel return locations

Backend behavior:

- Load the user’s active subscription
- Reject checkout if the user already has an active paid subscription
- Reject checkout if the user has no bound device
- Resolve the requested payment provider from the provider registry
- Ask that provider to create a recurring checkout session
- Return a normalized checkout response to the frontend

Design intent:

- Business validation is centralized before the provider call
- Provider-specific session creation is delegated to the provider implementation
- The frontend receives one consistent response shape regardless of provider

## 3. Provider-Side Checkout

After checkout creation, the user completes payment on the provider side.

Important design point:

- Local entitlement is not granted merely because checkout was created
- The system waits for asynchronous provider confirmation

This is a deliberate reliability choice. Checkout initiation is not treated as proof of payment.

## 4. Webhook as the Source of Truth

After the provider confirms billing activity, it sends webhook events back to the backend.

This is the most important part of the design.

The webhook layer is used to:

- Confirm that a subscription truly exists
- Confirm that an invoice was actually paid
- Update local state even when the frontend is offline
- Handle renewals, cancellations, and provider-driven state transitions

### Why Webhooks Are Critical

Without webhooks, the backend would depend too heavily on frontend redirect completion, which is not reliable enough for recurring billing.

Webhooks solve several real problems:

- The user may close the browser before returning
- The frontend may never receive the final redirect
- Renewals happen later and must still update local state
- Subscription changes can happen entirely outside the app

## Stripe Flow in Detail

The current implementation is strongest on Stripe.

### A. Checkout Session Completed

When checkout completion is received:

- Metadata is used to identify the local user and requested business plan
- The provider subscription object is fetched
- The backend checks whether the external subscription was already processed
- A local `UserSubscription` record is either upgraded or created

There is an important design choice here:

- If a free subscription record already exists, the system upgrades that record instead of always inserting a new paid row

Why this matters:

- It preserves continuity of account state
- It avoids unnecessary fragmentation of subscription history
- It allows the paid period to be added on top of the existing entitlement baseline

After the local subscription is updated:

- Device expiration data is aligned with the account subscription period
- If a paid invoice is already available, a `Payment` record is written
- The user’s premium-access flag is refreshed

### B. Invoice Paid

When a paid invoice event is received:

- The backend resolves the related external subscription
- It finds the local `UserSubscription`
- It checks for idempotency using the external payment reference
- It inserts a `Payment` record only if it has not already been written

Design intent:

- Checkout completion and payment confirmation are not collapsed into one fragile assumption
- Payment history is recorded from invoice truth, which is better aligned with recurring billing

### C. Subscription Updated

When the provider reports a subscription status update:

- The backend loads the local subscription by external subscription reference
- It normalizes the provider status into a smaller local lifecycle vocabulary
- If the subscription is effectively canceled or unpaid, it downgrades the user to the free plan
- Otherwise, it updates cancellation-at-period-end and status fields
- It refreshes the premium-access flag on the user account

Design intent:

- Keep the local state model simple and application-focused
- Avoid leaking provider-specific status complexity into the rest of the system

### D. Subscription Deleted

When the provider indicates that the subscription is deleted:

- The backend downgrades the local account to the free plan
- It preserves the most relevant period-end information when available
- It refreshes premium access

This makes deletion a clear terminal transition from the application point of view.

## Cancel and Resume Flow

The module supports self-service cancel and resume behavior for recurring billing.

### Cancel

The user can request cancellation of automatic renewal.

Backend behavior:

- Load the active subscription
- Reject if there is no valid paid subscription
- Reject if the subscription channel does not support this action in the current implementation
- Ask the provider to cancel at period end
- Update the local record to reflect that cancellation has been scheduled

Design intent:

- Preserve the user’s access until the end of the already paid billing cycle
- Stop future renewal rather than forcing immediate revocation

### Resume

If the subscription was only marked for end-of-period cancellation, the user can restore renewal.

Backend behavior:

- Ensure the subscription exists
- Ensure the provider channel supports resume
- Ensure the subscription is currently marked as cancel-at-period-end
- Ask the provider to clear that flag
- Update the local record accordingly

Design intent:

- Resuming renewal should be a lightweight state reversal
- It should not create a new checkout or trigger a new purchase immediately

## Scheduled Reliability Mechanisms

The design does not rely on webhooks alone.

Two scheduled mechanisms exist to improve reliability.

### 1. Subscription State Reconciliation

A background task periodically scans local paid subscriptions and asks the provider for their latest state.

Purpose:

- Recover from missed webhooks
- Repair local drift after temporary failures
- Downgrade accounts whose provider-side subscriptions no longer exist or are no longer valid

This is a classic defense-in-depth mechanism.

### 2. Expiry Reminder Notifications

A second task finds subscriptions close to their end date and sends reminder emails.

Purpose:

- Give users advance notice before access expires
- Reduce surprise churn
- Improve renewal conversion and support experience

## Access Control Coupling

This module is not only a billing module. It is also an entitlement module.

The subscription state affects:

- The user’s premium-access flag
- Device expiration alignment
- Runtime access checks for device-facing functionality

This is an intentional design choice: payment events are converted into application-level access state quickly and directly.

## Security Design

The security model follows several important principles.

### Webhook Endpoints Are Public but Verified

Webhook endpoints bypass user JWT authentication because they are called by payment providers, not by logged-in users.

Security is preserved by:

- Provider-side signature verification
- Event parsing under strict provider control
- Refusing to trust unsigned or malformed webhook traffic

### Secrets Are Not Part of Business Logic

Provider credentials, webhook secrets, and plan mappings are stored in configuration objects and are not embedded into the domain logic.

This keeps the core workflow readable and reduces accidental leakage risk.

### Local Idempotency

The webhook code checks whether a subscription or payment record has already been processed before inserting duplicates.

This is critical because webhook delivery is at-least-once by nature.

## Failure Handling Strategy

The module is designed around eventual consistency rather than fragile synchronous certainty.

Examples:

- Checkout creation does not grant access
- Webhooks update local state asynchronously
- Scheduled reconciliation repairs missed updates
- Payment insertions are idempotent
- Free-plan downgrade is used as a safe fallback state

This is a practical design for recurring billing systems.

## Why the Free Plan Exists as a Real Local Record

The system does not treat “free” as the complete absence of a subscription row.

Instead, it creates a real local free subscription record for new users.

This simplifies multiple behaviors:

- Upgrade from free to paid
- Downgrade from paid back to free
- A single place to reason about entitlement lifecycle
- Cleaner access-control logic

This is a useful modeling choice because it makes subscription transitions explicit.

## Current Implementation Status

Based on the code structure, the module is not equally complete across all providers.

### Implemented More Completely

- Stripe checkout creation
- Stripe webhook verification
- Stripe subscription lifecycle updates
- Local payment history persistence
- Local entitlement refresh
- Scheduled Stripe reconciliation

### Present but Not Fully Implemented

- PayPal checkout flow
- PayPal webhook verification and lifecycle handling

This means the architecture is multi-provider by design, but operational maturity is currently stronger on Stripe.

## Key Design Decisions and Their Rationale

### 1. Provider Abstraction Instead of Conditional Sprawl

Using a `PaymentProvider` interface prevents the main subscription service from becoming a long sequence of provider-specific condition branches.

### 2. Local Business Plans Separate from Provider Products

The system uses business-level plan identifiers and maps them to provider-side billing objects indirectly.

This is the right abstraction because:

- Internal product naming remains stable
- Provider migration becomes easier
- The frontend does not need provider-specific identifiers

### 3. Webhooks Drive Truth, Not Redirect Success

This is the most important billing design decision in the module.

It is the correct choice for recurring billing because renewals and cancellations are inherently asynchronous.

### 4. Local Entitlement Is Updated Immediately After Verified Events

The module updates user and device access state right after verified webhook processing.

That reduces lag between billing truth and product behavior.

### 5. Scheduled Reconciliation Is Kept as a Safety Net

Relying on webhooks alone is operationally risky. The scheduled sync task reduces that risk significantly.

## Recommended Mental Model

The simplest way to understand this module is:

- `SubscriptionPlan` defines what can be bought
- `PaymentProvider` defines how a provider checkout is created
- `UserSubscription` defines what the user is currently entitled to
- `Payment` records what has actually been charged
- Webhooks translate provider events into local truth
- Scheduled jobs keep local truth healthy over time

## Summary

This subscription module is designed around a sound recurring-billing architecture:

- checkout is provider-specific
- entitlement is local
- webhook events are authoritative
- persistence is explicit
- access state is synchronized to users and devices
- reconciliation exists as a fallback

In practical terms, the design is already strong for Stripe and structurally prepared for additional providers, even though not every provider path is equally complete yet.
