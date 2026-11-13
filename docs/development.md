# Development

How to build, run, and test ulearn locally, and how CI is wired.

## Prerequisites

- Java 21+ (CI uses Temurin 21)
- Node 22+ and npm
- Docker (for Compose and backend integration tests via Testcontainers)

## Repository layout

```
backend/    Spring Boot API (Gradle, Java 21)
frontend/   React + TypeScript SPA (Vite)
scripts/    setup, build, test, lint, run, clean
docs/       this guide, ADRs, agent docs
openspec/   specifications and change proposals
.github/    CI, CodeQL, Dependabot, PR template
```

## Configuration

Copy `.env.example` to `.env` (the setup script does this). Variables:

| Variable | Purpose |
| --- | --- |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Postgres database and credentials |
| `YOUTUBE_API_KEY` | Server-side YouTube Data API key |
| `BOOTSTRAP_USERNAME`, `BOOTSTRAP_PASSWORD` | Interim single-user credentials (ADR-0008) |
| `CATALOG_FRESHNESS_THRESHOLD` | ISO-8601 duration before a Playlist snapshot is refreshed (default `PT168H`) |
| `PROGRESS_COMPLETION_THRESHOLD` | Fraction of a Lesson's duration that marks it complete automatically (default `0.95`) |
| `VITE_API_BASE_URL` | API base URL baked into the frontend build |

Secrets live in `.env` (gitignored) and in GitHub Actions secrets for CI. Never commit them.

## Scripts

| Command | Does |
| --- | --- |
| `make setup` | Checks tools, creates `.env`, installs backend and frontend deps |
| `make build` | Builds backend and frontend |
| `make test` | Runs backend tests (Testcontainers Postgres) and frontend tests |
| `make lint` | Frontend lint + `openspec validate --strict` |
| `make run` | `docker compose up --build` |
| `make clean` | Removes build output, deps, containers, and volumes |

Each script is idempotent and also runnable directly under `scripts/`.

## Docker

`docker compose up --build` starts three services:

- `db` — postgres:16-alpine with a named volume and healthcheck
- `backend` — multi-stage image (Temurin 21), non-root, `/health` healthcheck
- `frontend` — multi-stage build served by nginx with SPA fallback

All configuration comes from environment variables; no secrets are baked into images.

## Continuous integration

`.github/workflows/ci.yml` runs on pull requests and pushes to `main` with four jobs:

| Job | Runs |
| --- | --- |
| `backend` | `./gradlew --no-daemon build` (Temurin 21, Gradle cache); fails on coverage below threshold, ArchUnit, Checkstyle, or formatting violations |
| `frontend` | `npm ci`, `npm run lint`, `npm test` (coverage-enforced), `npm run build` |
| `openspec` | installs `@fission-ai/openspec@1.17.0`, `openspec validate --strict` |
| `docker` | `docker compose build` |

Both `backend` and `frontend` publish their coverage HTML as workflow artifacts (`backend-coverage`, `frontend-coverage`); the `backend` job also uploads the Checkstyle report (`backend-checkstyle`).

Additional free automations:

- `.github/workflows/codeql.yml` — CodeQL analysis (Java + TypeScript)
- `.github/dependabot.yml` — weekly Gradle, npm, and Actions updates

### Required checks and secrets

To gate merges, enable branch protection on `main` and require the four CI checks (`backend`, `frontend`, `openspec`, `docker`). This is free for public repositories.

Provide `YOUTUBE_API_KEY`, `BOOTSTRAP_USERNAME`, and `BOOTSTRAP_PASSWORD` as GitHub Actions secrets if any job needs them; the current jobs do not require secrets.

## Testing notes

- Backend integration tests use Testcontainers, so Docker must be running.
- Extend `com.ulearn.BaseIntegrationTest` for backend integration tests rather than repeating `@SpringBootTest`/`@AutoConfigureMockMvc`/`@ActiveProfiles`/`@Import(TestcontainersConfiguration.class)`. The shared annotations keep tests on one cached application context; subclasses add `@Transactional` when they need rollback and `@MockitoBean` for collaborators. Tests that need genuinely different configuration (for example explicit properties or extra `@Import`s) may still extend the base and add those annotations, but avoid declaring a new context when the base would do.
- Frontend tests use Vitest with Testing Library and jsdom.

## Coverage gates

Unit-test coverage is enforced in CI, so a PR with untested code fails before review. Start thresholds are intentionally below the current level (a ratchet, not a target) and should be raised as the suite matures.

### Backend

- JaCoCo runs on every `test`; `jacocoTestCoverageVerification` is wired into `check`, so `./gradlew --no-daemon build` fails when **line coverage drops below 80%**.
- The Spring Boot bootstrap class `com.ulearn.UlearnBackendApplication` is excluded (nothing to test).
- HTML/XML reports land in `backend/build/reports/jacoco/test/`.
- Run locally: `cd backend && ./gradlew --no-daemon check` (or `jacocoTestReport` for the report only).

### Frontend

- Vitest's v8 provider runs with `npm test`; the run fails when **statements, branches, functions, or lines drop below 80%**.
- Excluded from coverage: test files (`*.test.ts(x)`), `src/test/`, and the `src/main.tsx` bootstrap.
- HTML/lcov reports land in `frontend/coverage/`.
- Run locally: `cd frontend && npm test` (or `npx vitest run --coverage`).

Thresholds and exclusions live in `backend/build.gradle.kts` and `frontend/vite.config.ts`.

### Mutation testing (PIT)

Coverage shows what executed, not whether the tests assert anything — an assertion-free test can reach
100% line coverage while catching nothing (verified: such a test scores 0% under PIT). [PIT](https://pitest.org/)
closes that gap by mutating production bytecode and checking whether the suite fails.

- Run locally: `cd backend && ./gradlew --no-daemon pitest`; HTML/XML reports land in
  `backend/build/reports/pitest/`.
- Scope: `targetClasses = com.ulearn.*` (the capability packages); generated/Lombok code and
  `UlearnBackendApplication` are excluded.
- Thresholds are ratchets, not targets: `mutationThreshold = 70`, `coverageThreshold = 80` — kept below
  the current score and raised as the suite matures.
- `pitest` is deliberately **not** wired into `build`/`check`, so the everyday build stays fast. It is
  enforced as a dedicated step in the `backend` CI job (`.github/workflows/ci.yml`); the full run takes
  well under a minute locally. If runtime grows, move it to a scheduled job or PIT's incremental
  analysis (issue #14).
- Versions are pinned: `info.solidsoft.pitest` 1.19.0, PIT 1.22.1, `pitest-junit5-plugin` 1.2.2.

## Architecture tests

`backend/src/test/java/com/ulearn/arch/ArchitectureTest.java` uses [ArchUnit](https://www.archunit.org/) to encode the modular-monolith structure from `design.md`, so the package boundaries fail the build when they erode. The rules:

- **Capability isolation** — the `accounts`, `catalog`, `progress`, `plans`, `compilations`, and `sharing` capabilities may depend on each other only through `com.ulearn.<capability>.api`; reaching into another capability's internals is a violation.
- **Direction of `common`/`config`** — `com.ulearn.common` and `com.ulearn.config` must not depend on any capability.
- **No cycles** — top-level packages under `com.ulearn` must be free of dependency cycles.
- **Layering** — `@RestController` classes must not depend on repositories or JPA entities; `@Configuration` lives in `..config..`, `@RestController` in `..web..`, `@Repository` in `..persistence..`.

Rules whose packages do not exist yet (the capabilities, controllers, repositories) are allowed to match no classes and start enforcing as those classes land. Add capability names to `ArchitectureTest` when a new one is introduced. Run locally with `cd backend && ./gradlew --no-daemon test --tests 'com.ulearn.arch.ArchitectureTest'`.

## Code style

The backend follows the [Google Java Style](https://google.github.io/styleguide/javaguide.html) through two complementary gates, both wired into `check`, so `./gradlew --no-daemon build` fails on a violation:

- **Checkstyle** runs the *Google checks* bundled with Checkstyle (`google_checks.xml` from the pinned `toolVersion` in `backend/build.gradle.kts`), promoted from their default `warning` severity to `error`. `checkstyleMain` covers `src/main`, `checkstyleTest` covers `src/test`.
- **Spotless** runs [google-java-format](https://github.com/google/google-java-format), the reference formatter for the same rules, so fixes are mechanical and reproducible.

Run locally (from `backend/`):

- Check style and formatting: `./gradlew --no-daemon spotlessCheck checkstyleMain checkstyleTest` (or just `check`, which also runs tests and the other gates).
- Fix formatting automatically: `./gradlew --no-daemon spotlessApply`.
- Reports land in `backend/build/reports/checkstyle/{main,test}.html`.

Justified exemptions live in `backend/config/checkstyle/suppressions.xml`. Today it disables only the two Javadoc *presence* checks (`MissingJavadocMethod`, `MissingJavadocType`) — this codebase documents intent in names and tests rather than method-level Javadoc — while keeping Google's Javadoc *quality* rules. There is no `ignoreFailures` and no baseline: every remaining rule must pass.

The frontend keeps its existing ESLint gate (`npm run lint`).
