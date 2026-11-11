# ADR-0001: Track progress in the embedded YouTube player

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

Users want automatic study-progress tracking. The original `progress-tracking` spec described syncing the user's YouTube watch activity through the YouTube Data API using their granted access. The public Data API exposes no watch history and no per-video watch position, so that mechanism is not implementable.

## Decision

Track progress automatically while the user watches a Lesson inside ulearn, using the YouTube IFrame Player API. The player reports playback state and elapsed time; the backend records completion when watched time reaches a configured threshold of the Lesson duration. No OAuth watch-activity access is used.

## Consequences

- Automatic tracking covers only playback inside ulearn; watching the same video on YouTube directly is not detected.
- Manual progress adjustment is required to correct or fill gaps.
- The `progress-tracking` spec no longer references watch activity or OAuth authorization.
- Backend gets a progress-report endpoint fed by the player; the frontend owns the player integration.
