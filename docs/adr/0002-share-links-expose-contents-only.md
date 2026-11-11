# ADR-0002: Share links expose contents only

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

Courses and Compilations can be shared via public links. It was unspecified what a visitor sees beyond the contents: whether progress is shown, and whether a visitor can copy the shared item into their own account.

## Decision

A share link exposes only the structure of a Course or Compilation (its Lessons, order, titles) in read-only form. It exposes no one's progress, and grants no import/copy capability. To study a shared item, a visitor adds the source playlist themselves.

## Consequences

- Shared views leak no per-user data.
- "Import a shared course" is out of scope for this change; if added later it becomes a new capability with its own spec.
- The `sharing` spec explicitly excludes progress and import in the public read-only view.
