# Progress API

Tracks a user's progress through the Lessons of their Courses. Automatic tracking happens while a
Lesson plays in ulearn's embedded YouTube player: the player reports elapsed watch time on a
heartbeat, and the backend marks the Lesson complete once watched time reaches a configured
threshold of its duration (ADR-0001). Because the YouTube Data API exposes no watch history or
per-video position, watching a Lesson directly on YouTube is **not** detected; the user can adjust
progress manually to correct the record.

All endpoints require authentication (interim HTTP Basic; see `docs/api/accounts.md`) and act only on
the authenticated owner's Courses. Paths are Course-scoped so the progress capability can validate
that the Lesson belongs to an owned Course.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/courses/{courseId}/progress` | Course progress and per-Lesson states |
| `POST` | `/api/courses/{courseId}/lessons/{lessonId}/heartbeat` | Record automatic playback progress |
| `PUT` | `/api/courses/{courseId}/lessons/{lessonId}/progress` | Set a manual completion mark |
| `DELETE` | `/api/courses/{courseId}/lessons/{lessonId}/progress` | Clear a manual mark |

### `POST /api/courses/{courseId}/lessons/{lessonId}/heartbeat`

Records automatic progress from the embedded player. The watched time is **monotonic** — a report
never lowers the maximum watched time — and repeated reports are idempotent. When a manual mark is
present, heartbeats do not change the completion state (the manual mark wins) but the maximum watched
time still updates.

```http
POST /api/courses/3f0c2b1a-9d4e-4c8b-8f2a-1b2c3d4e5f60/lessons/1a2b3c4d-0000-0000-0000-000000000001/heartbeat HTTP/1.1
Authorization: Basic <base64(BOOTSTRAP_USERNAME:BOOTSTRAP_PASSWORD)>
Content-Type: application/json

{ "watchedSeconds": 95 }
```

Response `200 OK`:

```json
{
  "lessonId": "1a2b3c4d-0000-0000-0000-000000000001",
  "state": "COMPLETE",
  "completed": true,
  "manualOverride": false,
  "maxWatchedSeconds": 95
}
```

`state` is `IN_PROGRESS` or `COMPLETE`. A Lesson is complete when `maxWatchedSeconds` reaches
`PROGRESS_COMPLETION_THRESHOLD` (default `0.95`) of the Lesson's duration. A Lesson with no known
duration is never completed automatically. `watchedSeconds` must be `>= 0`.

### `PUT /api/courses/{courseId}/lessons/{lessonId}/progress`

Sets a manual completion mark. The mark **takes precedence** over automatic tracking until it is
cleared, so later heartbeats cannot override it. Marking `completed: false` records an explicit
manual "not complete". Unavailable Lessons (videos no longer in the source playlist) cannot be played,
so a manual mark is the only way to complete them; they still count toward Course progress (ADR-0004).

```http
PUT /api/courses/{courseId}/lessons/{lessonId}/progress HTTP/1.1
Content-Type: application/json

{ "completed": true }
```

Returns the updated Lesson progress (`200`). `completed` is required.

### `DELETE /api/courses/{courseId}/lessons/{lessonId}/progress`

Clears a manual mark, returning the Lesson to automatic tracking (derived from the maximum watched
time). Returns the updated Lesson progress (`200`).

### `GET /api/courses/{courseId}/progress`

Returns the Course's progress and every Lesson's state.

```json
{
  "courseId": "3f0c2b1a-9d4e-4c8b-8f2a-1b2c3d4e5f60",
  "progressPercent": 50.0,
  "completedLessons": 1,
  "totalLessons": 2,
  "complete": false,
  "lessons": [
    {
      "lessonId": "1a2b3c4d-0000-0000-0000-000000000001",
      "state": "COMPLETE",
      "completed": true,
      "manualOverride": false,
      "maxWatchedSeconds": 95
    },
    {
      "lessonId": "1a2b3c4d-0000-0000-0000-000000000002",
      "state": "IN_PROGRESS",
      "completed": false,
      "manualOverride": false,
      "maxWatchedSeconds": 0
    }
  ]
}
```

- **Course progress** = completed lessons ÷ **all** lessons (available and unavailable), as a
  percentage. Unavailable lessons count in both the numerator and the denominator.
- `progressPercent` is `null` only when the playlist is empty (0/0) — progress is undefined, not
  `0` or `100`.
- `complete` is `true` only when there is at least one Lesson and every Lesson is complete.
- Progress is computed on read from the Course's current lesson set, so a re-sync that adds a Lesson
  recomputes progress and can revoke completion; a re-sync that removes a Lesson keeps the Lesson
  (marked unavailable) and its progress.

## Errors

All errors use the standard `application/problem+json` shape.

| Status | When |
| --- | --- |
| `400 Bad Request` | Missing/invalid body (for example a negative `watchedSeconds` or missing `completed`) |
| `401 Unauthorized` | Missing or wrong credentials |
| `404 Not Found` | The Course does not exist, is soft-deleted, or belongs to another owner (`Course not found`), or the Lesson is not in that Course (`Lesson not found`) |

```json
{ "type": "about:blank", "title": "Lesson not found", "status": 404, "detail": "Lesson not found" }
```

## Data model

| Table | Role |
| --- | --- |
| `lesson_progress` | One row per `(user_id, playlist_lesson_id)`: `state`, `max_watched_seconds`, `manual_override`, timestamps (unique per user + lesson) |

Progress rows reference the Lesson by id and are never attached to the shared Playlist snapshot
(ADR-0005). Deleting a Lesson row (only on hard cleanup, ADR-0006) cascades to its progress rows.

## Configuration

| Variable | Purpose |
| --- | --- |
| `PROGRESS_COMPLETION_THRESHOLD` | Fraction of a Lesson's duration that marks it complete automatically (default `0.95`) |
