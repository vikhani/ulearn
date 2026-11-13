# Development flow

The end-to-end loop we follow for every change in this repo. Build/run/CI details live in `docs/development.md`.

## Stay in scope

While planning or implementing, you will notice problems that are **not** part of the change in hand. If the problem does not block the current change, do **not** fix it on the go:

- File a GitHub issue for it and keep the current branch focused (see `docs/agents/issue-tracker.md` for the `gh` commands).
- Link related issues/PRs (`Part of #<n>`, `Blocked by: #<n>`) and use glossary vocabulary in the title.
- Record deferred follow-ups in the change's `tasks.md` "Workflow follow-up" section so nothing is lost.

Only expand the current branch for work the change actually requires. Everything else waits in the issue tracker; fixing it here creates extra PRs and hides work that deserves its own review.

## 1. Plan with OpenSpec

- Every feature or bugfix starts as an OpenSpec change: `openspec/changes/<change-id>/` with `proposal.md`, `specs/`, `design.md` (when warranted), and `tasks.md`.
- Track milestone work as checkboxes in `tasks.md` and tick them off as you go.
- Validate before pushing: `openspec validate --changes --strict` (also covered by `make lint`).

## 2. Sharpen decisions into docs

- Run `/grill-with-docs` while planning. It lazily produces/updates `GLOSSARY.md` (domain terms) and `docs/adr/NNNN-*.md` (decisions).
- Use glossary vocabulary in code, tests, issues, and commit messages. Surface ADR conflicts instead of silently overriding them.

## 3. Branch

- Branch off an up-to-date `main`: `git checkout -b <topic>`.
- Never commit secrets: `.env` is gitignored, only `.env.example` is tracked.

## 4. Implement and verify locally

Run the same gates CI runs, before pushing:

- `make lint` — frontend lint + `openspec validate --strict`
- `make test` — backend (Testcontainers) + frontend (Vitest)
- `./gradlew --no-daemon build` — backend
- `docker compose build` — images

Docker must be running for Testcontainers.

## 5. Commit

- Small, focused commits with imperative, concise subjects; match the existing `git log` style.
- No secret values or generated secrets in commits.

## 6. Push and open a PR

- `git push -u origin <branch>`
- `gh pr create --fill` (body follows `.github/pull_request_template.md`), or pass `--title`/`--body` explicitly.

## 7. Review and respond

- Read feedback: `gh pr view <n> --comments`.
- List review-comment ids: `gh api repos/{owner}/{repo}/pulls/<n>/comments --jq '.[] | {id, path, body}'`
- Reply inline to a review comment: `gh api repos/{owner}/{repo}/pulls/<n>/comments/<comment-id>/replies -f body="..."`
- Fold fixes into the same branch as new commits; don't rewrite history unless asked.

## 8. Merge gate — only when CI is green

- `gh pr checks <n>` must show no `fail` or `pending` (jobs: `backend`, `frontend`, `openspec`, `docker`, plus CodeQL).
- Merge: `gh pr merge <n> --squash --delete-branch` (squash keeps `main` at one commit per PR).
- Then `git fetch --prune` and `git checkout main && git pull`.

## Conventions that bite

- Backend is **Gradle (Kotlin DSL)**; always use the `./gradlew` wrapper. In Gradle 9 the `-B` alias is gone — use `--no-daemon`.
- Permissions are scoped in `.opencode/opencode.jsonc` (JSONC, so rules are commented inline). This project allows `git push*` and `gh *` but asks on force pushes / pushes to `main`/`master` and denies destructive `gh` commands; `read`/`grep`/`glob` cannot touch secrets. Restart opencode after editing it.
