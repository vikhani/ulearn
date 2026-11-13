package com.ulearn.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulearn.BaseIntegrationTest;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import com.ulearn.catalog.persistence.CourseEntity;
import com.ulearn.catalog.persistence.CourseRepository;
import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import com.ulearn.catalog.youtube.ResolvedPlaylist;
import com.ulearn.catalog.youtube.ResolvedVideo;
import com.ulearn.catalog.youtube.YouTubeClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PlaylistSnapshotServiceTest extends BaseIntegrationTest {

  @Autowired PlaylistSnapshotService snapshotService;

  @Autowired PlaylistRepository playlistRepository;

  @Autowired CourseRepository courseRepository;

  @Autowired UserRepository userRepository;

  @MockitoBean YouTubeClient youTubeClient;

  @Test
  void reusesFreshSnapshotWithoutSecondApiCall() {
    when(youTubeClient.resolvePlaylist("PL-cache"))
        .thenReturn(snapshot("PL-cache", "Cached", video("v1", "One", 0, 60)));

    snapshotService.getOrCreateFresh("PL-cache");
    snapshotService.getOrCreateFresh("PL-cache");

    verify(youTubeClient, times(1)).resolvePlaylist("PL-cache");
    assertThat(playlistRepository.findByYoutubePlaylistId("PL-cache")).isPresent();
    assertThat(playlistRepository.count()).isEqualTo(1);
  }

  @Test
  void refreshesStaleSnapshot() {
    when(youTubeClient.resolvePlaylist("PL-stale"))
        .thenReturn(snapshot("PL-stale", "Stale", video("v1", "One", 0, 60)));

    PlaylistEntity playlist = snapshotService.getOrCreateFresh("PL-stale");
    playlist.markSynced(Instant.now().minus(Duration.ofDays(30)));
    playlistRepository.save(playlist);

    snapshotService.getOrCreateFresh("PL-stale");

    verify(youTubeClient, times(2)).resolvePlaylist("PL-stale");
  }

  @Test
  void persistsTwoCoursesReferencingOneSharedPlaylist() {
    when(youTubeClient.resolvePlaylist("PL-shared"))
        .thenReturn(snapshot("PL-shared", "Shared", video("v1", "One", 0, 60)));
    PlaylistEntity playlist = snapshotService.getOrCreateFresh("PL-shared");

    courseRepository.save(
        new CourseEntity(owner("owner-a"), playlist, "A", "https://youtube.com/a"));
    courseRepository.save(
        new CourseEntity(owner("owner-b"), playlist, "B", "https://youtube.com/b"));

    List<CourseEntity> courses = courseRepository.findAll();
    assertThat(courses).hasSize(2);
    assertThat(courses)
        .allSatisfy(course -> assertThat(course.getPlaylist().getId()).isEqualTo(playlist.getId()));
    assertThat(playlistRepository.count()).isEqualTo(1);
  }

  private UUID owner(String username) {
    return userRepository.save(new UserEntity(username, username)).getId();
  }

  private static ResolvedPlaylist snapshot(
      String playlistId, String title, ResolvedVideo... videos) {
    return new ResolvedPlaylist(playlistId, title, List.of(videos));
  }

  private static ResolvedVideo video(
      String videoId, String title, int position, int durationSeconds) {
    return new ResolvedVideo(videoId, title, position, durationSeconds);
  }
}
