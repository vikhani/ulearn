# ADR-0006: Soft-delete Courses; monthly cleanup with 90-day grace

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

Deleting a Course that other things reference (Compilations, Share links, plans) is ambiguous and risks breaking references or orphaning data.

## Decision

Deleting a Course soft-deletes it: it is hidden from the owner and stops being shared, while its references remain intact. A scheduled job runs **monthly** and hard-deletes Playlist snapshots no longer referenced by any Course after a **90-day grace period**.

## Consequences

- Deletion is effectively reversible within the grace period (no restore UI is promised yet).
- The global snapshot is reclaimed only when no Course references it.
- The job must be idempotent and safe to run on a single instance.
- Shares and Compilation membership pointing at a soft-deleted Course resolve as hidden rather than dangling.
