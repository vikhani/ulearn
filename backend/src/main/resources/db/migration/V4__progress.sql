CREATE TABLE lesson_progress (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users (id),
    playlist_lesson_id  UUID        NOT NULL REFERENCES playlist_lessons (id) ON DELETE CASCADE,
    state               TEXT        NOT NULL,
    max_watched_seconds INTEGER     NOT NULL DEFAULT 0,
    manual_override     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT lesson_progress_user_lesson_unique UNIQUE (user_id, playlist_lesson_id)
);

CREATE INDEX lesson_progress_user_id_idx ON lesson_progress (user_id);
CREATE INDEX lesson_progress_playlist_lesson_id_idx ON lesson_progress (playlist_lesson_id);
