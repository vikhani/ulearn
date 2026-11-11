# Design

## Context

Greenfield project; see proposal.md for motivation and `specs/` for the behavior contract. The repository currently contains only OpenSpec scaffolding, so this design establishes the architecture the whole service will follow.

Fixed constraints from the user:

- Backend in **Java + Spring Boot**.
- Frontend is our choice.
- Playlists are supplied by the user as URLs; the app resolves and refreshes playlist/video data using a **server-side YouTube API key**.
- Accounts and authentication, since courses, progress, plans, and shares are per-user. **Staged**: the first version uses HTTP Basic auth with a single user from `.env`; the multi-user `user-accounts` spec is the target (ADR-0008).
- **Automatic** progress tracking via YouTube.

A key external constraint shapes the progress design: the YouTube **Data API v3 does not expose a user's watch history or per-video watch position**. There is no endpoint that reports "user X watched video Y to 70%". Any automatic-tracking design that depends on reading watch history is not implementable against the public API.

## Goals / Non-Goals

**Goals:**

- A modular monolith REST backend that is straightforward to deploy and evolve.
- Per-user ownership enforced consistently across every resource.
- Automatic progress tracking that is actually achievable with YouTube's public APIs.
- Share links that are unguessable, revocable, and leak no private data.

**Non-Goals:**

- Reading the user's native YouTube watch history (not supported by the API).
- Multi-tenant organizations, teams, or role-based admin.
- Offline playback or video hosting (video always streams from YouTube).
- Native mobile apps; the web SPA is the client.
- Recommendation/search across YouTube beyond resolving user-supplied URLs.

## Decisions

### Backend: Spring Boot modular monolith

Java 21 + Spring Boot 4 (built with Gradle), exposing a REST/JSON API. Package by capability (`accounts`, `catalog`, `progress`, `plans`, `compilations`, `sharing`) rather than by layer, so each capability owns its controller/service/repository.

- **Why**: matches the user's stack choice; a single deployable keeps the MVP operationally simple while the capability packages preserve clear boundaries.
- **Alternatives**: microservices per capability (rejected: operational overhead with no MVP benefit); reactive WebFlux (rejected: blocking JDBC/JPA is simpler and sufficient for this load profile).

### Persistence: PostgreSQL + Spring Data JPA + Flyway

PostgreSQL for storage, Spring Data JPA for access, Flyway for versioned migrations. Aggregates (course + lessons, compilation + members) are modeled as owning relationships.

- **Why**: relational ownership/foreign-key constraints enforce per-user scoping and referential integrity; Flyway gives reproducible schema evolution.
- **Alternatives**: MongoDB (rejected: relationships and per-user constraints are naturally relational); JPA `ddl-auto` without migrations (rejected: unsafe for real deployments).

### Auth: staged — Basic auth now, multi-user target

The first version authenticates with **HTTP Basic** against a single user whose credentials come from `.env`; there is no registration or session store yet. The `user-accounts` spec (registration, login, sessions, profile) is the multi-user target and is implemented in a follow-up change. Ownership is derived from the authenticated principal, never from client-supplied user IDs, so capabilities are unaffected by the later swap.

- **Why**: gets the MVP running with minimal surface while keeping the multi-user contract intact (ADR-0008).
- **Alternatives**: full JWT access + refresh now (rejected for the first cut: more surface than needed); server-side sessions (deferred with the multi-user work). The interim Basic-auth path is replaced, not extended.

### Automatic progress via the embedded YouTube IFrame Player API

Lessons play inside ulearn using the **YouTube IFrame Player API**. The player reports playback state and elapsed watch time; the frontend posts progress to the backend on a heartbeat and on pause/end. The backend marks a lesson complete when watched time reaches a configurable threshold of its duration.

- **Why**: this is the only way to deliver *automatic* tracking with YouTube's public APIs. The Data API v3 exposes no watch history or watch position, so reading history is impossible; controlling playback in our own player lets us observe real watch progress without it.
- **Consequence (externally observable)**: automatic tracking applies to lessons watched **in ulearn**. Watching the same video directly on YouTube is not detected.
- **Alternatives**: Data API watch-history polling (rejected: endpoint does not exist); requiring users to grant OAuth for "watch activity" (rejected: no such scope returns watch history). The `progress-tracking` spec's **manual progress adjustment** requirement covers anything the player cannot observe and lets users correct the record.

### Progress writes are idempotent and monotonic-safe

`lesson_progress` is keyed by `(user_id, lesson_id)` with a unique constraint. Heartbeat updates upsert; completion is derived from the maximum watch time seen. Manual marks are stored as an explicit override flag that automatic updates do not clear.

- **Why**: the player heartbeat can fire repeatedly and out of order; the spec requires sync to be idempotent and manual marks to take precedence.
- **Alternatives**: append-only event log (deferred: useful later for analytics, unnecessary now).

### YouTube Data API integration: server-side key, global snapshot

The backend resolves playlist and video metadata with a server-side API key (never sent to the client). Playlist data is stored once, globally, as a **Playlist snapshot** that per-user Courses reference (ADR-0005). Snapshots are refreshed on add, on owner-triggered re-sync, and by a **weekly** job for stale snapshots (ADR-0009); pages are fetched in batches to conserve quota.

- **Why**: the Data API is quota-limited; storing once and batching avoid burning quota when many users add the same playlist.
- **Alternatives**: per-user copies (rejected: wasteful); per-request live fetch (rejected: quota and latency); client-side key (rejected: leaks the secret).

### Sharing: random opaque token, stored hashed, revocable

Share links use a cryptographically random token (≥128 bits) over HTTPS. Only a hash of the token is stored; the plaintext appears once, in the returned URL. Revocation marks the share record revoked; setting a resource back to private revokes its links.

- **Why**: unguessable, revocable, and a database leak does not expose usable tokens.
- **Alternatives**: signed JWTs as share tokens (rejected: hard to revoke before expiry without a denylist); sequential IDs (rejected: guessable).

### Frontend: React + TypeScript SPA (Vite)

A React + TypeScript single-page app built with Vite, using TanStack Query for server state and the YouTube IFrame Player API for lesson playback.

- **Why**: clean separation from the Java API; strong typing; the player integration is a small, well-supported JS surface.
- **Alternatives**: Next.js (rejected: server rendering adds no value for an authenticated SPA backed by a separate API); server-rendered templates (rejected: poorer fit for an interactive player).

### One repository, scripted Docker build, free GitHub Actions CI

All components live in a single repository with documented setup/build scripts and Docker definitions (Dockerfiles + Compose) for a reproducible local stack. CI and pull-request checks run on GitHub Actions' free tier: build, tests, lint, and `openspec validate`, with secrets from GitHub Actions secrets.

- **Why**: satisfies the single-repo, one-command, automated-checks requirement without paid CI; Compose gives every developer the same stack.
- **Alternatives**: separate repositories per component (rejected: contradicts the single-repo requirement); Jenkins/self-hosted CI (rejected: cost and ops overhead); paid runners or GitHub features outside the free plan (rejected: must stay free).

### Data model (principal entities)

- `users`, `credentials` (BCrypt hash) — for now a single bootstrap user from `.env` (ADR-0008)
- `playlists` (global snapshot: YouTube playlist id, title, last synced at) — shared, never user-visible (ADR-0005)
- `playlist_lessons` (playlist, position, video id, title, duration, available)
- `courses` (owner, playlist ref, name, visibility, deleted at) — per-user (ADR-0005, ADR-0006)
- `lesson_progress` (user, playlist lesson, state, max watched seconds, manual override)
- `goals` (course, target date, target percent)
- `study_plans` (course, target date, weekly commitment, start date)
- `compilations` (owner, title, visibility)
- `compilation_courses` (compilation, course, position)
- `shares` (token hash, resource type + id, owner, revoked at)

## Risks / Trade-offs

- **Automatic tracking only covers playback inside ulearn** → Document it in the UI and provide manual adjustment (already specified) so progress can be corrected.
- **YouTube API quota exhaustion** → Cache playlist data, batch requests, make re-sync explicit, and return retriable errors without corrupting data.
- **Playlist content drifts over time (removed/private videos)** → Re-sync marks lessons unavailable and recomputes progress; completion can be revoked by new lessons.
- **Compilation aggregate progress semantics** → Weight by all lessons across member courses (ADR-0007) so a large course is not under-represented.
- **Single-user Basic auth for now** → Capabilities key off "the authenticated user"; the multi-user swap stays local to accounts (ADR-0008).
- **Scheduled jobs (weekly refresh, monthly cleanup)** → Must be idempotent, single-instance-safe, and quota-aware (ADR-0006, ADR-0009).
- **Share token leakage via referrers/logs** → Store only hashes, send tokens over HTTPS, keep them out of server logs, and support revocation.
- **JWT revocation before expiry** → Keep access tokens short-lived and revoke via refresh-token invalidation; accept the brief window as a trade-off for statelessness.

## Migration Plan

- Greenfield: no data migration. Deploy order is backend + database first, then frontend.
- Flyway runs schema migrations on startup; the schema is additive so rollback is a redeploy of the previous image plus a reverse migration only if a release is reverted.
- YouTube API key and the bootstrap user's credentials are provided via environment/secret configuration, never committed.

## Open Questions

- Freshness threshold for the weekly refresh job (a tunable value; does not change specs).
- Whether to add OAuth sign-in (Google) alongside email/password in the multi-user follow-up — deferred.
