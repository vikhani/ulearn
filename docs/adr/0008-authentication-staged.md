# ADR-0008: Authentication staged — Basic auth now, multi-user target

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

The `user-accounts` spec describes full multi-user accounts (registration, login, sessions, profile). The first implementation should be minimal, with a single bootstrap user supplied via environment variables.

## Decision

Implement the first version with HTTP Basic authentication against a single user defined in `.env`. The `user-accounts` spec remains the multi-user target and is **not** weakened; full registration, login, sessions, and profile are a follow-up change. Every capability continues to operate on "the authenticated user", so switching to multi-user does not change their contracts.

## Consequences

- For now there is effectively one identity; sharing still works but shares that single user's Courses.
- The bootstrap password comes from environment configuration, never the repository.
- A later change implements the multi-user spec; the interim Basic-auth path is replaced, not extended.
- Capabilities must not assume a user id beyond "the authenticated user", so the swap stays local to the accounts capability.
