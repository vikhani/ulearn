# ADR-0009: Weekly refresh of stale Playlist snapshots

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

Globally-stored Playlist snapshots (ADR-0005) drift as playlists change. Refreshing too often burns YouTube API quota; refreshing only on user action leaves stale data.

## Decision

Track each snapshot's `last_synced_at`. A scheduled job runs **weekly** and refreshes snapshots that are not fresh (older than a freshness threshold), so stale playlists converge without hammering the API. Owner-triggered re-sync and add-time refresh remain available.

## Consequences

- Snapshot freshness is bounded by the threshold, not by user action.
- The job must respect API quota, batch requests, and be idempotent.
- After a refresh, progress is recomputed for affected Courses (new lessons can revoke completion).
