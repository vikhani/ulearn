# Tasks

## 1. Backend scaffolding and infrastructure

- [x] 1.1 Create the Spring Boot project (Java 21, Gradle) with capability packages `accounts`, `catalog`, `progress`, `plans`, `compilations`, `sharing`; verify it builds with `./gradlew build` and starts locally.
- [x] 1.2 Add PostgreSQL, Spring Data JPA, Flyway, validation, and security dependencies; verify a baseline `V1__init.sql` Flyway migration applies against a local database and the app boots without schema errors.
- [ ] 1.3 Configure the YouTube Data API key and the bootstrap user credentials via environment/secret config; verify the app reads them from config and fails fast with a clear message when either is absent.
- [x] 1.4 Add a health endpoint and a consistent JSON error response shape; verify `/health` returns OK and a forced validation error returns the standard error body in a test.

## 2. Authentication (staged: Basic auth)

- [ ] 2.1 Implement the single bootstrap user from `.env` (username + password) with HTTP Basic security; verify a request with correct credentials is authenticated and one with wrong credentials is rejected with 401.
- [ ] 2.2 Add the security filter chain deriving ownership from the authenticated principal; verify tests confirm the authenticated owner is allowed and access to a resource owned by another identity is denied.
- [ ] 2.3 Ensure secrets (bootstrap password) come from environment config and are never logged or committed; verify the app fails fast when credentials are absent and no secret appears in logs.
- [ ] 2.4 Document the interim auth model and the planned multi-user swap in `docs/api/accounts.md`; verify the documented behavior matches the running endpoints.

_(Full multi-user accounts — registration, login, sessions, profile — is deferred to a follow-up change; the `user-accounts` spec stays the target. See ADR-0008.)_

## 3. Playlist snapshot, catalog, and YouTube integration

- [ ] 3.1 Implement `playlists` and `playlist_lessons` persistence (global snapshot with `last_synced_at`) plus per-user `courses` referencing a playlist; verify a repository test persists two courses referencing one shared playlist.
- [ ] 3.2 Implement a YouTube Data API client that resolves a playlist URL to metadata and paginated video items using the server-side key; verify a mocked client test covers normal resolution, not-found/private, and quota/outage errors.
- [ ] 3.3 Implement "add playlist as course" including URL validation, non-playlist rejection, per-user duplicate detection, and reuse of an existing global snapshot (refreshing it if stale); verify tests cover accepted, non-playlist, duplicate, and cache-reuse scenarios from `specs/course-catalog/spec.md`.
- [ ] 3.4 Implement ordered lesson snapshotting (position, title, video id, duration) including the empty-playlist case; verify tests assert lesson order and the empty-playlist state.
- [ ] 3.5 Implement owner re-sync that adds new videos, marks removed videos unavailable (retaining metadata and a YouTube link), and preserves existing progress; verify tests cover new-video and removed-video re-sync scenarios.
- [ ] 3.6 Implement list/view/rename/delete for owned courses, with delete as soft-delete (hidden from the owner and from shares); verify tests cover owner-only listing, soft-delete hiding, and that a course in a compilation is unaffected for other members.
- [ ] 3.7 Implement batching to conserve quota; verify a test shows a repeated resolution is served from the stored snapshot without a second API call.
- [ ] 3.8 Implement the weekly refresh job that refreshes snapshots older than the freshness threshold and recomputes affected course progress; verify tests cover stale selection, idempotency, and progress recomputation (ADR-0009).
- [ ] 3.9 Implement the monthly cleanup job that hard-deletes snapshots unreferenced by any course after a 90-day grace period; verify tests cover the grace period and that referenced snapshots are never deleted (ADR-0006).
- [ ] 3.10 Document the catalog API and YouTube quota/caching/refresh behavior in `docs/api/catalog.md` and verify the documented examples against the running endpoints.

## 4. Progress tracking

- [ ] 4.1 Implement `lesson_progress` persistence keyed by `(user_id, playlist_lesson_id)` with a unique constraint and a manual-override flag; verify a migration/repository test enforces uniqueness and stores the override flag.
- [ ] 4.2 Implement an idempotent heartbeat/upsert endpoint that records max watched seconds and derives completion from a configurable duration threshold; verify tests cover watched-to-completion, partial watch, and repeated (idempotent) heartbeats.
- [ ] 4.3 Implement manual mark complete / clear with precedence over automatic tracking, including manual completion of unavailable lessons; verify tests cover a manual mark surviving a later update, unavailable-lesson manual completion, and clearing returning control to automatic.
- [ ] 4.4 Implement course progress = completed ÷ all lessons (available and unavailable), recomputation on lesson-set change, and the undefined (null) case only when the playlist is empty; verify tests cover each scenario in `specs/progress-tracking/spec.md`.
- [ ] 4.5 Implement course completion (complete only when every lesson is complete; revoked when new incomplete lessons appear); verify tests cover both scenarios.
- [ ] 4.6 Verify progress isolation between users with a test asserting one user's progress does not alter another's.
- [ ] 4.7 Document the progress API and the in-player tracking model in `docs/api/progress.md`, and verify the documented heartbeat payload matches the running endpoint.

## 5. Goals and study plans

- [ ] 5.1 Implement `goals` persistence (one active goal per course) and create/replace with target-date and percentage validation; verify tests cover creation, replacement, and invalid targets from `specs/study-plans/spec.md`.
- [ ] 5.2 Implement `study_plans` persistence with target date and weekly commitment, deriving required pace; verify tests cover plan creation with derived pace and rejection of a target date before start.
- [ ] 5.3 Implement plan-vs-actual status (on track/ahead/behind, shortfall, completed); verify tests cover behind, on track, and completed scenarios.
- [ ] 5.4 Implement list/update/delete for plans with owner scoping; verify tests cover update recomputation, deletion, and owner-only listing.
- [ ] 5.5 Document the plans/goals API in `docs/api/plans.md` and verify documented examples against the running endpoints.

## 6. Compilations

- [ ] 6.1 Implement `compilations` and `compilation_courses` persistence (ordered membership); verify a repository test persists a compilation with ordered member courses.
- [ ] 6.2 Implement create/list/view/rename/delete with owner scoping; verify tests cover creation, missing-title rejection, owner-only listing, and deletion not removing member courses.
- [ ] 6.3 Implement add/remove/reorder members with ownership and duplicate prevention; verify tests cover the added, not-owned, duplicate, and reorder scenarios from `specs/course-compilations/spec.md`.
- [ ] 6.4 Implement compilation progress = completed lessons ÷ total lessons across members (weighted), with undefined (null) when there are no lessons; verify tests cover weighted reporting and the empty case (ADR-0007).
- [ ] 6.5 Document the compilations API in `docs/api/compilations.md` and verify documented examples against the running endpoints.

## 7. Sharing

- [ ] 7.1 Implement `shares` persistence storing only a token hash with resource type/id, owner, and revoked timestamp; verify a repository test stores and looks up by token hash and never stores plaintext.
- [ ] 7.2 Implement share-link creation with a cryptographically strong token and owner-only authorization; verify tests cover creation, denial for non-owners, and token entropy/uniqueness.
- [ ] 7.3 Implement the public read-only view for courses and compilations that exposes contents only — no progress, no import — and excludes private owner data; verify tests cover anonymous access, progress-excluded, no-import, no-private-data, and invalid/revoked-token not-found responses.
- [ ] 7.4 Implement revocation and the private-visibility default, including revoking existing links when a resource is set back to private; verify tests cover revocation, owner-only revocation, default-private, and private-revokes-links scenarios.
- [ ] 7.5 Implement serving current contents through a valid link; verify a test shows owner updates are reflected on the next view.
- [ ] 7.6 Document the sharing API and token/visibility model in `docs/api/sharing.md` and verify documented examples against the running endpoints.

## 8. Frontend foundation and authentication

- [x] 8.1 Scaffold the React + TypeScript SPA with Vite, routing, TanStack Query, and an API client; verify `npm run build` succeeds and the app renders a shell route.
- [ ] 8.2 Implement Basic-auth login/logout against the bootstrap user and an auth guard; verify a test covers successful login, invalid credentials, and logout clearing the session.
- [ ] 8.3 Add authenticated routing/guards and a standard API error surface; verify a test shows unauthenticated users are redirected and API errors are displayed.

## 9. Frontend courses, player, and progress

- [ ] 9.1 Implement course list, add-course-by-URL form, course detail, and re-sync action; verify a test covers adding a valid URL, rejecting a non-playlist URL, and triggering re-sync.
- [ ] 9.2 Integrate the YouTube IFrame Player API for lesson playback and post heartbeat/completion progress to the backend; verify a test (with a mocked player) posts heartbeats and a completion update.
- [ ] 9.3 Implement manual lesson mark/clear controls (including unavailable lessons) and display per-course progress and completion state; verify a test covers manual marking overriding automatic state.
- [ ] 9.4 Surface the in-player tracking limitation (progress is tracked when watching in ulearn) in the lesson UI; verify the message is rendered on the course/lesson view.

## 10. Frontend plans, compilations, and sharing

- [ ] 10.1 Implement goal and study-plan creation/editing with on-track/ahead/behind display; verify a test covers creating a plan and rendering a behind-schedule state.
- [ ] 10.2 Implement compilation list/detail with add/remove/reorder of member courses and weighted progress display; verify a test covers adding and reordering members and rendering weighted progress.
- [ ] 10.3 Implement share-link creation/revocation and a public read-only shared view (contents only, no progress); verify a test covers generating a link, viewing it unauthenticated, and revocation making it inaccessible.

## 11. Integration and end-to-end verification

- [ ] 11.1 Run a backend end-to-end test that authenticates (Basic), adds a course (mocked YouTube), records progress, sets a plan, builds a compilation, and creates a share link; verify the full flow passes.
- [ ] 11.2 Run a frontend end-to-end test covering login → add course → play a lesson → see progress → share; verify it passes against a running backend.
- [ ] 11.3 Verify cross-cutting security: unauthorized access is denied, share tokens are not logged or leaked, and no secret appears in logs; verify by inspecting responses/logs and a negative-access test.
- [ ] 11.4 Confirm all `specs/*/spec.md` scenarios are covered by at least one test and record the mapping in `docs/testing.md`.

## 12. Repository, build automation, and CI

- [x] 12.1 Ensure all components live in one repository with a documented layout; verify a fresh clone contains backend, frontend, Docker definitions, scripts, and docs with no external repository dependency.
- [x] 12.2 Add idempotent setup and build scripts covering all components; verify running each script twice completes successfully and a clean machine can be set up and built with one command each.
- [x] 12.3 Add Dockerfiles and a Compose file for backend, frontend, and database with config via environment variables; verify `docker compose up` starts the full stack and no secret is baked into an image.
- [ ] 12.4 Add a GitHub Actions CI workflow (free tier) that builds and tests on pull requests and pushes; verify a PR shows the workflow status and a failing test fails the run.
- [ ] 12.5 Add required pull-request checks (build, tests, lint, and `openspec validate`) and mark them required in branch protection; verify a PR with a failing check cannot merge.
- [ ] 12.6 Store CI secrets in GitHub Actions secrets and confirm no credentials are committed; verify a secret-dependent job reads from secrets and a repo scan finds no secrets.
- [x] 12.7 Document the repository layout, scripts, Docker usage, and CI in `docs/development.md` and verify the documented commands run as written.

## Workflow follow-up

- Implement the full multi-user `user-accounts` spec (registration, login, sessions, profile) in a follow-up change, replacing the interim Basic auth (ADR-0008).
- Archive the change after review and successful end-to-end verification.
- Verify the archived result updates the main specs under `openspec/specs/`.
