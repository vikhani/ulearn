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
| `backend` | `./gradlew --no-daemon build` (Temurin 21, Gradle cache); fails on coverage below threshold |
| `frontend` | `npm ci`, `npm run lint`, `npm test` (coverage-enforced), `npm run build` |
| `openspec` | installs `@fission-ai/openspec@1.17.0`, `openspec validate --strict` |
| `docker` | `docker compose build` |

Both `backend` and `frontend` publish their coverage HTML as workflow artifacts (`backend-coverage`, `frontend-coverage`).

Additional free automations:

- `.github/workflows/codeql.yml` — CodeQL analysis (Java + TypeScript)
- `.github/dependabot.yml` — weekly Gradle, npm, and Actions updates

### Required checks and secrets

To gate merges, enable branch protection on `main` and require the four CI checks (`backend`, `frontend`, `openspec`, `docker`). This is free for public repositories.

Provide `YOUTUBE_API_KEY`, `BOOTSTRAP_USERNAME`, and `BOOTSTRAP_PASSWORD` as GitHub Actions secrets if any job needs them; the current jobs do not require secrets.

## Testing notes

- Backend integration tests use Testcontainers, so Docker must be running.
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
