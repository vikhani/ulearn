package com.ulearn.progress.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ulearn.BaseIntegrationTest;
import com.ulearn.accounts.persistence.UserRepository;
import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistLessonEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class LessonProgressPersistenceTest extends BaseIntegrationTest {

  @Autowired LessonProgressRepository lessonProgressRepository;

  @Autowired PlaylistRepository playlistRepository;

  @Autowired UserRepository userRepository;

  @Test
  void storesManualOverrideFlagAndState() {
    UUID userId = bootstrapUserId();
    UUID lessonId = persistLesson("v1");

    LessonProgressEntity progress = new LessonProgressEntity(userId, lessonId);
    progress.markManually(true);
    LessonProgressEntity saved = lessonProgressRepository.saveAndFlush(progress);

    LessonProgressEntity reloaded = lessonProgressRepository.findById(saved.getId()).orElseThrow();
    assertThat(reloaded.isManualOverride()).isTrue();
    assertThat(reloaded.isComplete()).isTrue();
    assertThat(reloaded.getMaxWatchedSeconds()).isZero();
  }

  @Test
  void rejectsSecondProgressRowForSameUserAndLesson() {
    UUID userId = bootstrapUserId();
    UUID lessonId = persistLesson("v1");

    lessonProgressRepository.saveAndFlush(new LessonProgressEntity(userId, lessonId));

    assertThatThrownBy(
            () -> lessonProgressRepository.saveAndFlush(new LessonProgressEntity(userId, lessonId)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private UUID bootstrapUserId() {
    return userRepository.findByUsername("test-user").orElseThrow().getId();
  }

  private UUID persistLesson(String videoId) {
    PlaylistEntity playlist = new PlaylistEntity("PL-" + videoId, "Study");
    PlaylistLessonEntity lesson = playlist.addLesson(videoId, "Lesson", 0, 90);
    playlistRepository.saveAndFlush(playlist);
    return lesson.getId();
  }
}
