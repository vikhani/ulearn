# Spec Delta

## Purpose

Keeps the whole service in one repository with a scripted, Docker-based build and setup, and runs CI and pull-request checks on free GitHub Actions so every change is built and tested automatically.

## ADDED Requirements

### Requirement: Single-repository layout

The project SHALL be contained in one repository holding all components — backend, frontend, infrastructure/Docker, scripts, and documentation — and MUST NOT depend on another repository to build or run.

#### Scenario: Fresh clone is self-contained
- **WHEN** the repository is cloned fresh
- **THEN** all components needed to build and run the service are present in that single clone

#### Scenario: No external repository required
- **WHEN** the build and setup scripts run
- **THEN** they resolve every dependency from the repository or public registries, not from a sibling repository

### Requirement: Automated build and setup scripts

The repository SHALL provide documented scripts that set up and build the whole project with a single command each, and the scripts SHALL be idempotent.

#### Scenario: One-command setup
- **WHEN** a developer runs the documented setup script on a supported machine
- **THEN** the script prepares the environment and installs prerequisites without manual steps

#### Scenario: One-command build
- **WHEN** a developer runs the documented build script
- **THEN** it builds all components (backend and frontend) and reports success or a clear failure

#### Scenario: Idempotent re-run
- **WHEN** the setup or build script is run again after a successful run
- **THEN** it completes without error and without duplicating prior work

### Requirement: Containerized build and runtime

The project SHALL provide Docker definitions (Dockerfiles and/or a Compose file) that build and run the full stack, with configuration supplied through environment variables and no secrets baked into images.

#### Scenario: Full stack via Compose
- **WHEN** a developer runs the documented Docker Compose command
- **THEN** the backend, frontend, and database start together and the service is reachable

#### Scenario: Reproducible images
- **WHEN** images are built from the same source revision
- **THEN** the resulting images are equivalent, with pinned base images and dependencies

#### Scenario: Configuration via environment
- **WHEN** the stack starts
- **THEN** secrets and configuration come from environment variables, and no secret is embedded in an image or committed

### Requirement: Continuous integration

The project SHALL run continuous integration on GitHub Actions using only free features, triggered on pull requests and pushes, that builds and tests the code.

#### Scenario: CI runs on pull request
- **WHEN** a pull request is opened or updated
- **THEN** a CI workflow builds and tests the affected components and reports a status

#### Scenario: Failing tests fail CI
- **WHEN** a build or test fails
- **THEN** the CI run fails and the failure is visible on the pull request

#### Scenario: Free-tier only
- **WHEN** CI runs
- **THEN** it uses only GitHub features available on the free plan, such as standard GitHub-hosted runners

### Requirement: Pull-request checks gate merges

The project SHALL define pull-request checks (build, tests, and lint, plus OpenSpec validation) and SHALL require them to pass before a pull request can merge.

#### Scenario: Checks required before merge
- **WHEN** a pull request has a failing or missing required check
- **THEN** the pull request cannot be merged until the check passes

#### Scenario: Spec validation is a check
- **WHEN** a pull request changes OpenSpec artifacts
- **THEN** OpenSpec validation runs as a check and fails the pull request when the artifacts are invalid

### Requirement: CI configuration and secrets

CI SHALL obtain secrets from GitHub's secret storage and MUST NOT require secrets for the build and tests that can run without them.

#### Scenario: Secrets from GitHub Secrets
- **WHEN** a CI job needs a secret
- **THEN** it reads it from GitHub Actions secrets, not from a committed file

#### Scenario: No secrets in the repository
- **WHEN** the repository is inspected
- **THEN** it contains no credentials or API keys
