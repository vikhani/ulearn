# Proposal

## Why

YouTube is a huge source of learning content, but it offers no way to structure a study path across playlists: there is no course model, no progress tracking, no goals, and no way to share a curated path. Learners currently juggle browser tabs, spreadsheets, and bookmarks. `ulearn` turns YouTube playlists into structured, trackable, shareable study courses.

## What Changes

- Users authenticate; all study data is scoped to a user. The first version uses Basic auth with a single bootstrap user from `.env`; full multi-user accounts are the target (ADR-0008).
- Users add a YouTube playlist by URL. The service resolves it through the YouTube Data API (server-side API key), stores the playlist once as a global **Playlist snapshot**, and each user gets their own **course** referencing it with ordered **lessons** (ADR-0005).
- Playlist and video metadata is re-synced on demand, on add, and weekly for stale snapshots so courses stay current (ADR-0009).
- The service tracks per-lesson progress automatically while the user watches inside ulearn's embedded player, and derives per-course progress and completion. Manual adjustment covers unavailable lessons and anything the player cannot observe (ADR-0001, ADR-0004).
- Users set **goals** and **study plans** (target dates, pace, weekly commitment) for their courses and see plan-vs-actual progress.
- Users group multiple courses into **compilations** (curated bundles of playlists).
- Users **share** courses and compilations through public, revocable links, with per-item visibility controls.

## Capabilities

### New Capabilities

- `user-accounts`: Account registration, login/authentication, session/token lifecycle, and user profile; every other capability is scoped to an authenticated user. Target state; the first version uses interim Basic auth (ADR-0008).
- `course-catalog`: Adding YouTube playlists as courses by URL, resolving them through the YouTube Data API, storing a global playlist snapshot, snapshotting ordered lessons, and re-syncing course content.
- `progress-tracking`: Automatic per-lesson progress tracked in ulearn's embedded player, plus manual adjustment and course-level progress and completion state.
- `study-plans`: Goals and study plans attached to courses (target date, pace, weekly commitment) with plan-vs-actual tracking.
- `course-compilations`: Grouping multiple courses into ordered compilations (curated playlist bundles).
- `sharing`: Public, revocable share links for courses and compilations with visibility controls.
- `build-and-delivery`: Single-repository layout, scripted and Docker-based build/setup, and free GitHub Actions CI and pull-request checks.

### Modified Capabilities

<!-- None: this is a greenfield project with no existing specs. -->

## Impact

- **New system**: greenfield web service named `ulearn`.
- **Backend**: Java + Spring Boot REST API; PostgreSQL for persistence; server-side YouTube Data API v3 integration (API key); scheduled weekly refresh and monthly cleanup jobs.
- **Frontend**: React + TypeScript single-page app consuming the REST API and embedding the YouTube IFrame Player for playback.
- **External dependencies**: YouTube Data API v3 (quota-limited) and the YouTube IFrame Player API.
- **Key constraint**: YouTube Data API v3 exposes no watch history or per-video position, so automatic progress is tracked in ulearn's embedded player; watching on YouTube directly is not detected (ADR-0001).
- **Repository**: one monorepo holding backend, frontend, Docker definitions, setup/build scripts, and docs.
- **CI/CD**: GitHub Actions (free tier) running build, tests, lint, and OpenSpec validation on pull requests.
- **Security**: API keys and the bootstrap user's credentials must be stored as secrets; share links must be unguessable and revocable.
