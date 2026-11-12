# Accounts API

Authentication for ulearn. The first version is **staged**: it uses HTTP Basic against a single
bootstrap user whose credentials come from the environment. The multi-user `user-accounts` spec
(registration, login, sessions, profile) is the target and is implemented in a follow-up change
(ADR-0008). The interim Basic-auth path is **replaced**, not extended.

## Interim model (current)

- One identity: the bootstrap user from `BOOTSTRAP_USERNAME` / `BOOTSTRAP_PASSWORD` (see
  `docs/development.md` → Configuration).
- Credentials are read from environment configuration at startup. The app fails fast with a clear
  message when `BOOTSTRAP_USERNAME` or `BOOTSTRAP_PASSWORD` is absent, and the password is never
  logged or committed.
- On startup the bootstrap account is **seeded idempotently** into the database (see Storage): the
  `users` row is created once, and its credential is stored only as a **BCrypt** hash. No plaintext is
  persisted.
- Every request except the health endpoints requires authentication. All study data is scoped to this
  authenticated owner.

## Storage

The `users` and `credentials` tables back identity so user-owned data (Courses, progress, plans,
compilations, shares) can reference a real user row (`user-accounts` spec; `design.md` data model):

- `users` — `id` (UUID), `username` (unique), `display_name`, `created_at`.
- `credentials` — `user_id` (FK to `users`), `password_hash` (BCrypt), `updated_at`.

The bootstrap user is seeded from environment config on every start: the user row is created only if
absent, and its credential hash is refreshed to match the configured password. Authentication loads
the stored hash through `DatabaseUserDetailsService`. A `courses.owner_id`-style FK to `users` keeps
per-user data tied to a real identity, so the multi-user swap does not have to migrate owner columns.

## Endpoints

### `GET /api/account`

Returns the currently authenticated identity. Use it to confirm Basic-auth credentials.

Request:

```http
GET /api/account HTTP/1.1
Authorization: Basic <base64(BOOTSTRAP_USERNAME:BOOTSTRAP_PASSWORD)>
```

Response `200 OK`:

```json
{ "id": "3f0c2b1a-9d4e-4c8b-8f2a-1b2c3d4e5f60", "username": "admin" }
```

Response `401 Unauthorized` when credentials are missing, wrong, or malformed. The body uses the
standard problem shape (`application/problem+json`):

```json
{ "status": 401, "title": "Unauthorized", "detail": "Authentication is required" }
```

### Public endpoints

- `GET /health`, `GET /health/**`, `GET /info` — actuator probes, no authentication.

## Ownership

Capabilities never trust a client-supplied user id. The current owner is derived from the
authenticated principal through the accounts capability's API:

- `com.ulearn.accounts.api.CurrentUserProvider#currentUser()` — the authenticated user's `id` and
  `username`, or an `AccessDeniedException` when there is none.
- `com.ulearn.accounts.api.OwnershipGuard#requireOwner(UUID)` — asserts the authenticated user's id is
  the resource owner, otherwise an `AccessDeniedException` (surfaced as `403 Forbidden`).

Capabilities store the owner's id and call `requireOwner` before serving a request, so per-user
isolation is enforced in one place and the multi-user swap stays local to the accounts capability.


## Configuration

| Variable | Purpose |
| --- | --- |
| `BOOTSTRAP_USERNAME` | Bootstrap user's username |
| `BOOTSTRAP_PASSWORD` | Bootstrap user's password (BCrypt-hashed in memory; never logged) |

## Planned multi-user swap

The follow-up change replaces the `SecurityConfig` wiring above with registration, email/password
login, an expiring session token, and profile management, per the `user-accounts` spec. Ownership
call sites (`CurrentUserProvider`, `OwnershipGuard`) keep their contracts, so other capabilities do
not change.
