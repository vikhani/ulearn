package com.ulearn.progress.internal;

import com.ulearn.accounts.api.CurrentUserProvider;
import com.ulearn.catalog.api.CourseCatalog;
import com.ulearn.catalog.api.CourseView;
import com.ulearn.catalog.api.LessonView;
import com.ulearn.config.ProgressProperties;
import com.ulearn.progress.api.CourseProgressView;
import com.ulearn.progress.api.LessonNotFoundException;
import com.ulearn.progress.api.LessonProgressView;
import com.ulearn.progress.api.ProgressState;
import com.ulearn.progress.api.ProgressTracking;
import com.ulearn.progress.persistence.LessonProgressEntity;
import com.ulearn.progress.persistence.LessonProgressRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives a user's progress from the embedded player's heartbeats and explicit manual marks. Course
 * progress is computed on read from the Course's current lesson set (via {@link CourseCatalog}) and
 * the stored {@code lesson_progress} rows, so a re-sync that changes the lesson set is reflected
 * without any recomputation step.
 */
@Service
@RequiredArgsConstructor
class ProgressService implements ProgressTracking {

  private final LessonProgressRepository lessonProgressRepository;
  private final CourseCatalog courseCatalog;
  private final CurrentUserProvider currentUserProvider;
  private final ProgressProperties progressProperties;

  @Override
  @Transactional
  public LessonProgressView recordHeartbeat(UUID courseId, UUID lessonId, int watchedSeconds) {
    LessonView lesson = requireOwnedLesson(courseId, lessonId);
    LessonProgressEntity progress = loadOrCreate(currentOwnerId(), lessonId);
    progress.recordAutomatic(
        watchedSeconds, lesson.durationSeconds(), progressProperties.getCompletionThreshold());
    return toView(lessonProgressRepository.save(progress));
  }

  @Override
  @Transactional
  public LessonProgressView markLesson(UUID courseId, UUID lessonId, boolean completed) {
    requireOwnedLesson(courseId, lessonId);
    LessonProgressEntity progress = loadOrCreate(currentOwnerId(), lessonId);
    progress.markManually(completed);
    return toView(lessonProgressRepository.save(progress));
  }

  @Override
  @Transactional
  public LessonProgressView clearLessonMark(UUID courseId, UUID lessonId) {
    LessonView lesson = requireOwnedLesson(courseId, lessonId);
    LessonProgressEntity progress = loadOrCreate(currentOwnerId(), lessonId);
    progress.clearManual(lesson.durationSeconds(), progressProperties.getCompletionThreshold());
    return toView(lessonProgressRepository.save(progress));
  }

  @Override
  @Transactional(readOnly = true)
  public CourseProgressView getCourseProgress(UUID courseId) {
    UUID ownerId = currentOwnerId();
    CourseView course = courseCatalog.getCourse(courseId);
    Map<UUID, LessonProgressEntity> byLesson = progressByLesson(ownerId, course.lessons());
    List<LessonProgressView> lessons =
        course.lessons().stream()
            .map(lesson -> toView(lesson.id(), byLesson.get(lesson.id())))
            .toList();
    int total = lessons.size();
    int completed = (int) lessons.stream().filter(LessonProgressView::completed).count();
    Double progressPercent = total == 0 ? null : (completed * 100.0) / total;
    boolean complete = total > 0 && completed == total;
    return new CourseProgressView(courseId, progressPercent, completed, total, complete, lessons);
  }

  private UUID currentOwnerId() {
    return currentUserProvider.currentUser().id();
  }

  private LessonView requireOwnedLesson(UUID courseId, UUID lessonId) {
    return courseCatalog.getCourse(courseId).lessons().stream()
        .filter(lesson -> lesson.id().equals(lessonId))
        .findFirst()
        .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));
  }

  private LessonProgressEntity loadOrCreate(UUID ownerId, UUID lessonId) {
    return lessonProgressRepository
        .findByUserIdAndPlaylistLessonId(ownerId, lessonId)
        .orElseGet(() -> new LessonProgressEntity(ownerId, lessonId));
  }

  private Map<UUID, LessonProgressEntity> progressByLesson(UUID ownerId, List<LessonView> lessons) {
    List<UUID> lessonIds = lessons.stream().map(LessonView::id).toList();
    if (lessonIds.isEmpty()) {
      return Map.of();
    }
    return lessonProgressRepository.findByUserIdAndPlaylistLessonIdIn(ownerId, lessonIds).stream()
        .collect(Collectors.toMap(LessonProgressEntity::getPlaylistLessonId, Function.identity()));
  }

  private static LessonProgressView toView(LessonProgressEntity progress) {
    return new LessonProgressView(
        progress.getPlaylistLessonId(),
        progress.getState(),
        progress.isComplete(),
        progress.isManualOverride(),
        progress.getMaxWatchedSeconds());
  }

  private static LessonProgressView toView(UUID lessonId, LessonProgressEntity progress) {
    if (progress == null) {
      return new LessonProgressView(lessonId, ProgressState.IN_PROGRESS, false, false, 0);
    }
    return toView(progress);
  }
}
