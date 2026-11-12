# Catalog API

Turns a YouTube playlist URL into a user-owned Course. The server resolves the playlist through the
YouTube Data API using a server-side key, stores the playlist once as a global **Playlist snapshot**,
and gives each user their own **Course** that references it (ADR-0005). All endpoints require
authentication (interim HTTP Basic; see `docs/api/accounts.md`) and act only on the authenticated
owner's Courses.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/courses` | Add a Course from a YouTube playlist URL |
| `GET` | `/api/courses` | List the owner's active Courses |
| `GET` | `/api/courses/{id}` | View one Course with its ordered lessons |
| `PATCH` | `/api/courses/{id}` | Rename a Course |
| `DELETE` | `/api/courses/{id}` | Soft-delete a Course |
| `POST` | `/api/courses/{id}/resync` | Re-sync a Course against its source playlist |

### `POST /api/courses`

Adds a Course from a playlist URL. The service rejects non-playlist URLs, rejects a playlist the user
already added, and reuses an existing snapshot (refreshing it when stale) instead of re-fetching
untouched playlists.

```http
POST /api/courses HTTP/1.1
Authorization: Basic <base64(BOOTSTRAP_USERNAME:BOOTSTRAP_PASSWORD)>
Content-Type: application/json

{ "url": "https://www.youtube.com/playlist?list=PLabcdefghijklmnop" }
```

Response `201 Created`:

```json
{
  "id": "3f0c2b1a-9d4e-4c8b-8f2a-1b2c3d4e5f60",
  "name": "Advanced Kotlin",
  "sourceUrl": "https://www.youtube.com/playlist?list=PLabcdefghijklmnop",
  "playlistId": "PLabcdefghijklmnop",
  "playlistTitle": "Advanced Kotlin",
  "lessonCount": 2,
  "hasLessons": true,
  "createdAt": "2026-11-12T15:00:00Z",
  "lastSyncedAt": "2026-11-12T15:00:00Z",
  "lessons": [
    {
      "id": "1a2b3c4d-0000-0000-0000-000000000001",
      "position": 0,
      "title": "First lesson",
      "videoId": "vid1",
      "durationSeconds": 90,
      "available": true,
      "youtubeUrl": "https://www.youtube.com/watch?v=vid1"
    },
    {
      "id": "1a2b3c4d-0000-0000-0000-000000000002",
      "position": 1,
      "title": "Second lesson",
      "videoId": "vid2",
      "durationSeconds": 120,
      "available": true,
      "youtubeUrl": "https://www.youtube.com/watch?v=vid2"
    }
  ]
}
```

The Course name defaults to the playlist title and can be changed with `PATCH`. An empty playlist
creates a Course with `lessonCount`/`hasLessons` false and no lessons; course progress is undefined
(`null`) for it (`docs/api/progress.md`).

### `GET /api/courses`

Lists the owner's active Courses, newest first. List responses omit `lessons` (they are `[]`) and keep
the summary fields (`lessonCount`, `hasLessons`, `lastSyncedAt`). Soft-deleted Courses do not appear.

### `GET /api/courses/{id}`

Returns one owned Course with its ordered lessons. `404` when the Course does not exist or was
soft-deleted; `403` when it belongs to another owner.

### `PATCH /api/courses/{id}`

```http
PATCH /api/courses/{id} HTTP/1.1
Content-Type: application/json

{ "name": "Renamed course" }
```

Returns the updated Course (`200`). `name` is required and capped at 200 characters.

### `DELETE /api/courses/{id}`

Soft-deletes the Course (ADR-0006): it is hidden from the owner's list and stops being shared, while its
references stay intact. Returns `204 No Content`.

### `POST /api/courses/{id}/resync`

Re-syncs the Course's Playlist snapshot against YouTube. New videos appear as lessons; a video no longer
in the playlist is **retained and marked `available: false`** with its stored metadata and YouTube link
(ADR-0004); existing lessons keep their rows, so progress is preserved. Returns the updated Course.

## Errors

All errors use the standard `application/problem+json` shape.

| Status | When |
| --- | --- |
| `400 Bad Request` | Missing/invalid body, or a URL that is not a YouTube playlist URL |
| `401 Unauthorized` | Missing or wrong credentials |
| `403 Forbidden` | The Course belongs to another owner |
| `404 Not Found` | The Course does not exist or is soft-deleted |
| `409 Conflict` | The playlist is already one of the user's active Courses |
| `422 Unprocessable Content` | YouTube reports the playlist as nonexistent, private, or unavailable |
| `503 Service Unavailable` | YouTube Data API quota exhausted or a transient outage — retriable |

```json
{ "type": "about:blank", "title": "Invalid playlist URL", "status": 400, "detail": "The URL does not point to a YouTube playlist" }
```

## Data model

| Table | Role |
| --- | --- |
| `playlists` | Global Playlist snapshot: `youtube_playlist_id` (unique), `title`, `last_synced_at` |
| `playlist_lessons` | Ordered videos of a snapshot: `position`, `video_id`, `title`, `duration_seconds`, `available` (unique per playlist + video) |
| `courses` | Per-owner Course: `owner_id` (FK to `users`), `playlist_id` (FK), `name`, `source_url`, `visibility`, `deleted_at` |

A user owns at most one active Course per Playlist snapshot (a partial unique index on
`owner_id` + `playlist_id` where `deleted_at IS NULL`); a soft-deleted Course does not block re-adding
the same playlist. The same snapshot is shared by every Course that references it.

## YouTube quota, caching, and refresh

- The API key is **server-side only**; it is never returned to clients or logged.
- A playlist is resolved once and stored as a global snapshot. Adding a playlist another user already
  added **reuses** the snapshot without an API call when it is fresh; a stale snapshot is refreshed.
- Freshness is bounded by `ulearn.catalog.freshness-threshold` (default `PT168H`, one week). This makes
  repeated adds quota-free and matches the planned weekly refresh cadence (ADR-0009).
- Playlist items are paged at 50 per request; video durations are fetched in batches of up to 50 ids per
  request, keeping the quota cost of a large playlist predictable.
- Owner re-sync is explicit via `POST /api/courses/{id}/resync`. The scheduled weekly refresh and monthly
  cleanup jobs (ADR-0009, ADR-0006) are planned follow-ups and are not implemented yet.

## Configuration

| Variable | Purpose |
| --- | --- |
| `YOUTUBE_API_KEY` | Server-side YouTube Data API key (required) |
| `CATALOG_FRESHNESS_THRESHOLD` | ISO-8601 duration before a snapshot is stale (default `PT168H`) |
