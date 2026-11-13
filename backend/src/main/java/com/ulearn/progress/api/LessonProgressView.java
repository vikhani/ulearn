package com.ulearn.progress.api;

import java.util.UUID;

/**
 * A user's progress on one Lesson: its completion state, whether a manual mark currently overrides
 * automatic tracking, and the maximum watched time seen.
 */
public record LessonProgressView(
    UUID lessonId,
    ProgressState state,
    boolean completed,
    boolean manualOverride,
    int maxWatchedSeconds) {}
