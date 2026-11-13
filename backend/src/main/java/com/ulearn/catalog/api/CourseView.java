package com.ulearn.catalog.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A Course as returned to its owner: the user-owned fields plus a summary of the shared Playlist
 * snapshot it references. {@code lessons} is populated on detail responses and empty on list
 * responses. {@code hasLessons} is false for an empty playlist, where progress is undefined.
 */
public record CourseView(
    UUID id,
    String name,
    String sourceUrl,
    String playlistId,
    String playlistTitle,
    int lessonCount,
    boolean hasLessons,
    Instant createdAt,
    Instant lastSyncedAt,
    List<LessonView> lessons) {}
