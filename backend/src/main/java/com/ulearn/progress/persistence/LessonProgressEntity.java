package com.ulearn.progress.persistence;

import com.ulearn.progress.api.ProgressState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One user's progress on one Lesson, keyed by {@code (user_id, playlist_lesson_id)}. Automatic
 * heartbeats upsert the maximum watched time; a manual mark is an explicit override that automatic
 * updates never clear (ADR-0001). The row references the Lesson by id (not a JPA relation) so the
 * progress capability stays decoupled from the catalog capability.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "playlist_lesson_id"}))
public class LessonProgressEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "playlist_lesson_id", nullable = false)
  private UUID playlistLessonId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProgressState state;

  @Column(name = "max_watched_seconds", nullable = false)
  private int maxWatchedSeconds;

  @Column(name = "manual_override", nullable = false)
  private boolean manualOverride;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public LessonProgressEntity(UUID userId, UUID playlistLessonId) {
    this.userId = userId;
    this.playlistLessonId = playlistLessonId;
    this.state = ProgressState.IN_PROGRESS;
    this.maxWatchedSeconds = 0;
    this.manualOverride = false;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Records an automatic heartbeat: the watched time is monotonic (never decreases), and the state
   * is derived from the threshold unless a manual mark currently takes precedence.
   */
  public void recordAutomatic(int watchedSeconds, int durationSeconds, double threshold) {
    this.maxWatchedSeconds = Math.max(this.maxWatchedSeconds, watchedSeconds);
    if (!manualOverride) {
      this.state =
          reachesThreshold(durationSeconds, threshold)
              ? ProgressState.COMPLETE
              : ProgressState.IN_PROGRESS;
    }
    this.updatedAt = Instant.now();
  }

  /** Sets a manual mark that takes precedence over automatic tracking until cleared. */
  public void markManually(boolean completed) {
    this.manualOverride = true;
    this.state = completed ? ProgressState.COMPLETE : ProgressState.IN_PROGRESS;
    this.updatedAt = Instant.now();
  }

  /** Clears the manual mark, returning the Lesson to automatic tracking. */
  public void clearManual(int durationSeconds, double threshold) {
    this.manualOverride = false;
    this.state =
        reachesThreshold(durationSeconds, threshold)
            ? ProgressState.COMPLETE
            : ProgressState.IN_PROGRESS;
    this.updatedAt = Instant.now();
  }

  public boolean isComplete() {
    return state == ProgressState.COMPLETE;
  }

  private boolean reachesThreshold(int durationSeconds, double threshold) {
    return durationSeconds > 0 && maxWatchedSeconds >= threshold * durationSeconds;
  }
}
