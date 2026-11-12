package com.ulearn.catalog.api;

import java.util.UUID;

/**
 * A Lesson as surfaced within a Course: its position and metadata, whether the video is still present
 * in the source playlist, and a reference to its YouTube page (usable even when unavailable).
 */
public record LessonView(
        UUID id,
        int position,
        String title,
        String videoId,
        int durationSeconds,
        boolean available,
        String youtubeUrl) {

    public static String youtubeUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }
}
