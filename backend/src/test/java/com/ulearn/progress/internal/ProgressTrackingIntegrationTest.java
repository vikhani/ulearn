package com.ulearn.progress.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ulearn.TestcontainersConfiguration;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import com.ulearn.catalog.persistence.CourseEntity;
import com.ulearn.catalog.persistence.CourseRepository;
import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import com.ulearn.catalog.youtube.ResolvedPlaylist;
import com.ulearn.catalog.youtube.ResolvedVideo;
import com.ulearn.catalog.youtube.YouTubeClient;
import com.ulearn.progress.api.CourseProgressView;
import com.ulearn.progress.api.ProgressTracking;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ProgressTrackingIntegrationTest {

  private static final String PLAYLIST_ID = "PLabcdefghijklmnop";
  private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=" + PLAYLIST_ID;

  @Autowired MockMvc mockMvc;

  @Autowired CourseRepository courseRepository;

  @Autowired PlaylistRepository playlistRepository;

  @Autowired UserRepository userRepository;

  @Autowired ProgressTracking progressTracking;

  @MockitoBean YouTubeClient youTubeClient;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void recordsCompletionWhenWatchedToThreshold() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));

    mockMvc
        .perform(
            post(heartbeatUrl(courseId, lessonId("v1")))
                .with(basic())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"watchedSeconds\":95}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("COMPLETE"))
        .andExpect(jsonPath("$.completed").value(true))
        .andExpect(jsonPath("$.manualOverride").value(false))
        .andExpect(jsonPath("$.maxWatchedSeconds").value(95));
  }

  @Test
  void recordsInProgressForPartialWatch() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));

    mockMvc
        .perform(
            post(heartbeatUrl(courseId, lessonId("v1")))
                .with(basic())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"watchedSeconds\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.completed").value(false))
        .andExpect(jsonPath("$.maxWatchedSeconds").value(50));
  }

  @Test
  void repeatedHeartbeatsAreIdempotentAndMonotonic() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));
    UUID lessonId = lessonId("v1");

    heartbeat(courseId, lessonId, 50).andExpect(jsonPath("$.maxWatchedSeconds").value(50));
    heartbeat(courseId, lessonId, 50).andExpect(jsonPath("$.maxWatchedSeconds").value(50));
    heartbeat(courseId, lessonId, 30)
        .andExpect(jsonPath("$.maxWatchedSeconds").value(50))
        .andExpect(jsonPath("$.completed").value(false));
  }

  @Test
  void manualMarkTakesPrecedenceOverAutomaticTracking() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));
    UUID lessonId = lessonId("v1");

    markLesson(courseId, lessonId, true)
        .andExpect(jsonPath("$.completed").value(true))
        .andExpect(jsonPath("$.manualOverride").value(true));

    heartbeat(courseId, lessonId, 0)
        .andExpect(jsonPath("$.completed").value(true))
        .andExpect(jsonPath("$.manualOverride").value(true));
  }

  @Test
  void clearingManualMarkReturnsToAutomaticTracking() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));
    UUID lessonId = lessonId("v1");

    markLesson(courseId, lessonId, true).andExpect(jsonPath("$.completed").value(true));

    mockMvc
        .perform(delete(progressUrl(courseId, lessonId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manualOverride").value(false))
        .andExpect(jsonPath("$.completed").value(false))
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
  }

  @Test
  void manualCompletionOfUnavailableLessonCountsTowardProgress() throws Exception {
    when(youTubeClient.resolvePlaylist(PLAYLIST_ID))
        .thenReturn(snapshot(video("v1", "First", 0, 100), video("v2", "Second", 1, 100)))
        .thenReturn(snapshot(video("v1", "First", 0, 100)));
    UUID courseId = addCourse();
    resync(courseId);

    UUID unavailableLessonId = lessonId("v2");
    markLesson(courseId, unavailableLessonId, true).andExpect(jsonPath("$.completed").value(true));

    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLessons").value(2))
        .andExpect(jsonPath("$.completedLessons").value(1))
        .andExpect(jsonPath("$.progressPercent").value(closeTo(50.0, 0.001)))
        .andExpect(jsonPath("$.complete").value(false));
  }

  @Test
  void courseProgressIsCompletedOverAllLessons() throws Exception {
    UUID courseId =
        addCourse(
            video("v1", "First", 0, 100),
            video("v2", "Second", 1, 100),
            video("v3", "Third", 2, 100));

    markLesson(courseId, lessonId("v1"), true).andExpect(status().isOk());

    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLessons").value(3))
        .andExpect(jsonPath("$.completedLessons").value(1))
        .andExpect(jsonPath("$.progressPercent").value(closeTo(33.333, 0.01)))
        .andExpect(jsonPath("$.complete").value(false));
  }

  @Test
  void emptyPlaylistProgressIsUndefined() throws Exception {
    when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot());
    UUID courseId = addCourse();

    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLessons").value(0))
        .andExpect(jsonPath("$.completedLessons").value(0))
        .andExpect(jsonPath("$.progressPercent").doesNotExist())
        .andExpect(jsonPath("$.complete").value(false))
        .andExpect(jsonPath("$.lessons").isEmpty());
  }

  @Test
  void courseCompleteOnlyWhenAllLessonsComplete() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100), video("v2", "Second", 1, 100));

    markLesson(courseId, lessonId("v1"), true).andExpect(status().isOk());
    markLesson(courseId, lessonId("v2"), true).andExpect(status().isOk());

    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedLessons").value(2))
        .andExpect(jsonPath("$.progressPercent").value(closeTo(100.0, 0.001)))
        .andExpect(jsonPath("$.complete").value(true));
  }

  @Test
  void completionRevokedAndProgressRecomputedWhenResyncAddsNewLesson() throws Exception {
    when(youTubeClient.resolvePlaylist(PLAYLIST_ID))
        .thenReturn(snapshot(video("v1", "First", 0, 100)))
        .thenReturn(snapshot(video("v1", "First", 0, 100), video("v2", "Second", 1, 100)));
    UUID courseId = addCourse();

    markLesson(courseId, lessonId("v1"), true).andExpect(status().isOk());
    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(jsonPath("$.complete").value(true));

    resync(courseId);

    mockMvc
        .perform(get(courseProgressUrl(courseId)).with(basic()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLessons").value(2))
        .andExpect(jsonPath("$.completedLessons").value(1))
        .andExpect(jsonPath("$.complete").value(false))
        .andExpect(jsonPath("$.progressPercent").value(closeTo(50.0, 0.001)));
  }

  @Test
  void progressIsIsolatedBetweenUsers() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));
    markLesson(courseId, lessonId("v1"), true).andExpect(status().isOk());

    UserEntity secondUser = userRepository.save(new UserEntity("second-user", "Second User"));
    PlaylistEntity playlist = playlistRepository.findByYoutubePlaylistId(PLAYLIST_ID).orElseThrow();
    CourseEntity secondCourse =
        courseRepository.save(
            new CourseEntity(
                secondUser.getId(),
                playlist,
                "Second",
                "https://www.youtube.com/playlist?list=" + PLAYLIST_ID));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("second-user", "n/a", List.of()));

    CourseProgressView secondProgress = progressTracking.getCourseProgress(secondCourse.getId());
    assertThat(secondProgress.completedLessons()).isZero();
    assertThat(secondProgress.progressPercent()).isZero();
  }

  @Test
  void returnsNotFoundForUnknownLesson() throws Exception {
    UUID courseId = addCourse(video("v1", "First", 0, 100));

    mockMvc
        .perform(
            post(heartbeatUrl(courseId, UUID.randomUUID()))
                .with(basic())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"watchedSeconds\":10}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Lesson not found"));
  }

  private org.springframework.test.web.servlet.ResultActions heartbeat(
      UUID courseId, UUID lessonId, int watchedSeconds) throws Exception {
    return mockMvc
        .perform(
            post(heartbeatUrl(courseId, lessonId))
                .with(basic())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"watchedSeconds\":" + watchedSeconds + "}"))
        .andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.ResultActions markLesson(
      UUID courseId, UUID lessonId, boolean completed) throws Exception {
    return mockMvc
        .perform(
            put(progressUrl(courseId, lessonId))
                .with(basic())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":" + completed + "}"))
        .andExpect(status().isOk());
  }

  private void resync(UUID courseId) throws Exception {
    mockMvc
        .perform(post("/api/courses/" + courseId + "/resync").with(basic()))
        .andExpect(status().isOk());
  }

  private UUID addCourse(ResolvedVideo... videos) throws Exception {
    when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(videos));
    return addCourse();
  }

  private UUID addCourse() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/courses")
                    .with(basic())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
  }

  private UUID lessonId(String videoId) {
    return playlistRepository
        .findByYoutubePlaylistId(PLAYLIST_ID)
        .orElseThrow()
        .findLessonByVideoId(videoId)
        .orElseThrow()
        .getId();
  }

  private static String heartbeatUrl(UUID courseId, UUID lessonId) {
    return "/api/courses/" + courseId + "/lessons/" + lessonId + "/heartbeat";
  }

  private static String progressUrl(UUID courseId, UUID lessonId) {
    return "/api/courses/" + courseId + "/lessons/" + lessonId + "/progress";
  }

  private static String courseProgressUrl(UUID courseId) {
    return "/api/courses/" + courseId + "/progress";
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor basic() {
    return httpBasic("test-user", "test-password");
  }

  private static ResolvedPlaylist snapshot(ResolvedVideo... videos) {
    return new ResolvedPlaylist(PLAYLIST_ID, "Study", List.of(videos));
  }

  private static ResolvedVideo video(
      String videoId, String title, int position, int durationSeconds) {
    return new ResolvedVideo(videoId, title, position, durationSeconds);
  }
}
