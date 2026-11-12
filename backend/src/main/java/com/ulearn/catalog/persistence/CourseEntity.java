package com.ulearn.catalog.persistence;

import com.ulearn.catalog.api.CourseVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user-owned Course built from a global Playlist snapshot. Ownership, name, and visibility live here;
 * the playlist and its lessons are shared and never user-visible (ADR-0005). Deletion soft-deletes the
 * Course while its references stay intact (ADR-0006).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseVisibility visibility;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public CourseEntity(UUID ownerId, PlaylistEntity playlist, String name, String sourceUrl) {
        this.ownerId = ownerId;
        this.playlist = playlist;
        this.name = name;
        this.sourceUrl = sourceUrl;
        this.visibility = CourseVisibility.PRIVATE;
        this.createdAt = Instant.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
