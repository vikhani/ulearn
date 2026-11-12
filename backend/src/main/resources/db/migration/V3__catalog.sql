CREATE TABLE playlists (
    id                  UUID PRIMARY KEY,
    youtube_playlist_id TEXT        NOT NULL UNIQUE,
    title               TEXT        NOT NULL,
    last_synced_at      TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE playlist_lessons (
    id               UUID PRIMARY KEY,
    playlist_id      UUID        NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    position         INTEGER     NOT NULL,
    video_id         TEXT        NOT NULL,
    title            TEXT        NOT NULL,
    duration_seconds INTEGER     NOT NULL,
    available        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT playlist_lessons_playlist_video_unique UNIQUE (playlist_id, video_id)
);

CREATE TABLE courses (
    id          UUID PRIMARY KEY,
    owner_id    UUID        NOT NULL REFERENCES users (id),
    playlist_id UUID        NOT NULL REFERENCES playlists (id),
    name        TEXT        NOT NULL,
    source_url  TEXT        NOT NULL,
    visibility  TEXT        NOT NULL DEFAULT 'PRIVATE',
    created_at  TIMESTAMPTZ NOT NULL,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX courses_owner_id_idx ON courses (owner_id);

-- A user owns at most one active Course per Playlist snapshot; a soft-deleted Course does not
-- block re-adding the same playlist (ADR-0005, ADR-0006).
CREATE UNIQUE INDEX courses_owner_playlist_active_unique
    ON courses (owner_id, playlist_id)
    WHERE deleted_at IS NULL;
