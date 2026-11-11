# ADR-0004: Re-sync retains unavailable lessons, and they count

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

When a re-sync finds a video no longer in the source playlist, the spec allowed either marking the Lesson unavailable or removing it. The choice affects progress math and whether the course outline survives. A follow-up decision fixed the progress denominator.

## Decision

Retain the Lesson, mark it unavailable, keep its stored metadata (title, duration, video id), and expose a reference to the YouTube page. **All** lessons in the playlist — available and unavailable — count in the progress denominator. Unavailable lessons cannot be tracked automatically (the player cannot play them), but the user can mark them complete manually. Progress is undefined only when the playlist is empty.

## Consequences

- The course outline stays stable across re-syncs.
- Manual completion is the only way to complete an unavailable lesson.
- A Lesson can become available again if the video reappears in the playlist.
- Supersedes the earlier "excluded from progress math" wording.
