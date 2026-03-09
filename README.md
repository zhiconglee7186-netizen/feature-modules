#  Subscription Module
## What It Demonstrates

- Plan listing
- Checkout creation
- Provider abstraction
- Webhook-driven subscription activation
- Payment record persistence
- Subscription cancel and resume
- In-memory repositories for local state

## What It Does Not Include

- Real payment provider integration
- Real signature verification
- Real persistence layer
- Authentication
- Scheduling

## Package Layout

- `api`: REST controllers
- `app`: application services and gateway abstraction
- `domain`: core models and enums
- `infra`: in-memory repositories

## Intended Use

Use this module as:

- a teaching reference
- an architecture sketch
- a starting point for a clean-room reimplementation

Do not treat it as production-ready billing code.
