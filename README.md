# ulearn

Turn YouTube playlists into structured, trackable, shareable study courses.

## Status

Early development. Product behavior is specified under `openspec/`; the Spring Boot backend and React frontend are being built against those specs.

## Stack

- **Backend**: Java 21 + Spring Boot REST API, PostgreSQL, Flyway
- **Frontend**: React + TypeScript + Vite
- **Delivery**: Docker + Docker Compose, GitHub Actions (CI, CodeQL, Dependabot)

## Quickstart

Prerequisites: Java 21+, Node 22+, Docker.

```
make setup
make run
```

- Backend health: http://localhost:8080/health
- Frontend: http://localhost:5173

## Common tasks

```
make build    # build backend and frontend
make test     # run backend and frontend tests
make lint     # frontend lint + openspec validate --strict
make clean    # remove build output, deps, and containers
```

## Repository layout

```
backend/    Spring Boot API (Maven)
frontend/   React + TypeScript SPA (Vite)
scripts/    setup/build/test/lint/run/clean
docs/       development guide, ADRs, agent docs
openspec/   specifications and change proposals
```

## Documentation

- `docs/development.md` — local development, scripts, Docker, CI
- `docs/adr/` — architecture decisions
- `GLOSSARY.md` — domain vocabulary
- `openspec/` — specs and change proposals
