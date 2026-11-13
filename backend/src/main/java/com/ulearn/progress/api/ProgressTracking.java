package com.ulearn.progress.api;

import java.util.UUID;

/**
 * The progress capability's contract, scoped to the authenticated owner. Implementations derive the
 * owner from the authenticated principal (never from client input) and validate that the Lesson
 * belongs to an owned Course (ADR-0008).
 */
public interface ProgressTracking {

  /** Records an automatic heartbeat for a Lesson and returns its updated progress. */
  LessonProgressView recordHeartbeat(UUID courseId, UUID lessonId, int watchedSeconds);

  /** Sets a manual completion mark on a Lesson, taking precedence over automatic tracking. */
  LessonProgressView markLesson(UUID courseId, UUID lessonId, boolean completed);

  /** Clears a Lesson's manual mark, returning it to automatic tracking. */
  LessonProgressView clearLessonMark(UUID courseId, UUID lessonId);

  /** Computes an owned Course's progress and per-Lesson states. */
  CourseProgressView getCourseProgress(UUID courseId);
}
