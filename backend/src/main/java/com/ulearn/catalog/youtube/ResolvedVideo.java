package com.ulearn.catalog.youtube;

/**
 * A video resolved from a YouTube playlist: its identifier, title, playlist position, and duration.
 */
public record ResolvedVideo(String videoId, String title, int position, int durationSeconds) {}
