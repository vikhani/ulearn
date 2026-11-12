package com.ulearn.catalog.youtube;

import java.util.List;

/**
 * The metadata and ordered videos resolved for a YouTube playlist through the YouTube Data API.
 */
public record ResolvedPlaylist(String playlistId, String title, List<ResolvedVideo> videos) {}
