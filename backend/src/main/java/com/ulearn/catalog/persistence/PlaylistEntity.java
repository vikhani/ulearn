package com.ulearn.catalog.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The globally stored snapshot of a YouTube playlist and its ordered lessons. It is shared across
 * users and only referenced by Courses; progress is never attached to it (ADR-0005). {@code
 * lastSyncedAt} bounds how stale the snapshot may be (ADR-0009).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "playlists")
public class PlaylistEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "youtube_playlist_id", nullable = false, unique = true)
  private String youtubePlaylistId;

  @Column(nullable = false)
  private String title;

  @Column(name = "last_synced_at", nullable = false)
  private Instant lastSyncedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL)
  @OrderBy("position ASC")
  private List<PlaylistLessonEntity> lessons = new ArrayList<>();

  public PlaylistEntity(String youtubePlaylistId, String title) {
    this.youtubePlaylistId = youtubePlaylistId;
    this.title = title;
    Instant now = Instant.now();
    this.lastSyncedAt = now;
    this.createdAt = now;
  }

  public void rename(String title) {
    this.title = title;
  }

  public void markSynced(Instant syncedAt) {
    this.lastSyncedAt = syncedAt;
  }

  public Optional<PlaylistLessonEntity> findLessonByVideoId(String videoId) {
    return lessons.stream().filter(lesson -> lesson.getVideoId().equals(videoId)).findFirst();
  }

  public PlaylistLessonEntity addLesson(
      String videoId, String title, int position, int durationSeconds) {
    PlaylistLessonEntity lesson =
        new PlaylistLessonEntity(this, videoId, title, position, durationSeconds);
    lessons.add(lesson);
    return lesson;
  }
}
