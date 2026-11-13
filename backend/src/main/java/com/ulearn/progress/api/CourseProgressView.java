package com.ulearn.progress.api;

import java.util.List;
import java.util.UUID;

/**
 * A Course's progress: completed lessons over all lessons (available and unavailable), plus each
 * Lesson's state. {@code progressPercent} is {@code null} when the playlist is empty, where
 * progress is undefined (ADR-0004).
 */
public record CourseProgressView(
    UUID courseId,
    Double progressPercent,
    int completedLessons,
    int totalLessons,
    boolean complete,
    List<LessonProgressView> lessons) {}
