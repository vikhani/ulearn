# ADR-0005: Global Playlist snapshot, per-user Courses and progress

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

The same popular playlist may be added by many users. Fetching and storing it once per user wastes YouTube API quota and memory.

## Decision

Store the YouTube playlist and its videos once, globally, as a **Playlist snapshot** refreshed centrally. A user's **Course** is per-user and references that snapshot; ownership, renaming, visibility, Goals, Study Plans, and progress all live on the Course, never on the Playlist. Progress is never shared between users. Adding a playlist already cached reuses the snapshot (and may trigger a refresh) instead of re-fetching.

## Consequences

- Duplicate detection is per user (a user adding their own duplicate is told it is already in their courses); the Playlist is globally unique by YouTube id.
- A snapshot is retained while any Course references it; the cleanup job (ADR-0006) removes unreferenced snapshots.
- Progress and completion math operate on the user's Course, not the global snapshot.
- The user-facing duplicate check and the global cache are different concerns and must not be conflated.
