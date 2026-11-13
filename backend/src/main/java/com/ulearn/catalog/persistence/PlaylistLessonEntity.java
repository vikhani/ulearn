package com.ulearn.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single video belonging to a Playlist snapshot. It is retained across re-syncs: a video no longer in
 * the source playlist is marked {@code available = false} rather than removed, keeping its metadata and
 * any user progress keyed to this row (ADR-0004).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "playlist_lessons",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "video_id"}))
public class PlaylistLessonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @Column(nullable = false)
    private int position;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    PlaylistLessonEntity(PlaylistEntity playlist, String videoId, String title, int position, int durationSeconds) {
        this.playlist = playlist;
        this.videoId = videoId;
        this.title = title;
        this.position = position;
        this.durationSeconds = durationSeconds;
        this.available = true;
        this.createdAt = Instant.now();
    }

    /** Reflects a video present again in the source playlist, restoring it as available. */
    public void update(String title, int position, int durationSeconds) {
        this.title = title;
        this.position = position;
        this.durationSeconds = durationSeconds;
        this.available = true;
    }

    public void markUnavailable() {
        this.available = false;
    }
}
