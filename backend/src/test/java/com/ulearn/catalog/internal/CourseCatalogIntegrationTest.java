package com.ulearn.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ulearn.TestcontainersConfiguration;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import com.ulearn.catalog.persistence.CourseEntity;
import com.ulearn.catalog.persistence.CourseRepository;
import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistLessonEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import com.ulearn.catalog.youtube.ResolvedPlaylist;
import com.ulearn.catalog.youtube.ResolvedVideo;
import com.ulearn.catalog.youtube.YouTubeClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CourseCatalogIntegrationTest {

    private static final String PLAYLIST_ID = "PLabcdefghijklmnop";
    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=" + PLAYLIST_ID;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    PlaylistRepository playlistRepository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    YouTubeClient youTubeClient;

    @Test
    void addsAPlaylistAsACourseWithOrderedLessons() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID))
                .thenReturn(
                        snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90), video("v2", "Second", 1, 120)));

        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Study"))
                .andExpect(jsonPath("$.playlistId").value(PLAYLIST_ID))
                .andExpect(jsonPath("$.lessonCount").value(2))
                .andExpect(jsonPath("$.hasLessons").value(true))
                .andExpect(jsonPath("$.lessons[0].position").value(0))
                .andExpect(jsonPath("$.lessons[0].videoId").value("v1"))
                .andExpect(jsonPath("$.lessons[0].available").value(true))
                .andExpect(jsonPath("$.lessons[0].youtubeUrl").value("https://www.youtube.com/watch?v=v1"))
                .andExpect(jsonPath("$.lessons[1].position").value(1))
                .andExpect(jsonPath("$.lessons[1].videoId").value("v2"))
                .andExpect(jsonPath("$.lessons[1].youtubeUrl").value("https://www.youtube.com/watch?v=v2"));
    }

    @Test
    void rejectsANonPlaylistUrl() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid playlist URL"));

        assertThat(courseRepository.count()).isZero();
    }

    @Test
    void rejectsAMissingUrlBody() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsADuplicateCourseForTheSamePlaylist() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90)));

        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate course"));

        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    void createsACourseWithNoLessonsForAnEmptyPlaylist() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(PLAYLIST_ID, "Empty"));

        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonCount").value(0))
                .andExpect(jsonPath("$.hasLessons").value(false))
                .andExpect(jsonPath("$.lessons").isEmpty());
    }

    @Test
    void resyncAddsNewVideosAndMarksRemovedOnesUnavailable() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID))
                .thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90), video("v2", "Second", 1, 120)))
                .thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90), video("v3", "Third", 1, 60)));

        UUID courseId = addCourse();

        PlaylistEntity beforeResync = playlistRepository.findByYoutubePlaylistId(PLAYLIST_ID).orElseThrow();
        UUID retainedLessonId = beforeResync.findLessonByVideoId("v1").orElseThrow().getId();

        mockMvc.perform(post("/api/courses/" + courseId + "/resync").with(httpBasic("test-user", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonCount").value(3));

        PlaylistEntity afterResync = playlistRepository.findByYoutubePlaylistId(PLAYLIST_ID).orElseThrow();
        assertThat(afterResync.getLessons()).hasSize(3);
        assertThat(afterResync.findLessonByVideoId("v1").orElseThrow().getId())
                .as("retained lesson keeps its id so progress is preserved")
                .isEqualTo(retainedLessonId);
        assertThat(afterResync.findLessonByVideoId("v2").orElseThrow().isAvailable()).isFalse();
        assertThat(afterResync.findLessonByVideoId("v3")).isPresent();
    }

    @Test
    void listsOnlyActiveOwnedCourses() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90)));
        addCourse();

        mockMvc.perform(get("/api/courses").with(httpBasic("test-user", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Study"))
                .andExpect(jsonPath("$[0].lessons").isEmpty());
    }

    @Test
    void renamesAnOwnedCourse() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90)));
        UUID courseId = addCourse();

        mockMvc.perform(patch("/api/courses/" + courseId)
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void softDeletesAnOwnedCourseAndHidesIt() throws Exception {
        when(youTubeClient.resolvePlaylist(PLAYLIST_ID)).thenReturn(snapshot(PLAYLIST_ID, "Study", video("v1", "First", 0, 90)));
        UUID courseId = addCourse();

        mockMvc.perform(delete("/api/courses/" + courseId).with(httpBasic("test-user", "test-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses").with(httpBasic("test-user", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/courses/" + courseId).with(httpBasic("test-user", "test-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deniesAccessToACourseOwnedByAnotherIdentity() throws Exception {
        PlaylistEntity playlist = playlistRepository.save(new PlaylistEntity("PL-other", "Other"));
        UUID otherOwner = userRepository.save(new UserEntity("other-owner", "other-owner")).getId();
        CourseEntity course = courseRepository.save(
                new CourseEntity(otherOwner, playlist, "Other", "https://youtube.com/other"));

        mockMvc.perform(get("/api/courses/" + course.getId()).with(httpBasic("test-user", "test-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    private UUID addCourse() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(httpBasic("test-user", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + PLAYLIST_URL + "\"}"))
                .andExpect(status().isCreated());
        return courseRepository.findAll().get(0).getId();
    }

    private static ResolvedPlaylist snapshot(String playlistId, String title, ResolvedVideo... videos) {
        return new ResolvedPlaylist(playlistId, title, List.of(videos));
    }

    private static ResolvedVideo video(String videoId, String title, int position, int durationSeconds) {
        return new ResolvedVideo(videoId, title, position, durationSeconds);
    }
}
