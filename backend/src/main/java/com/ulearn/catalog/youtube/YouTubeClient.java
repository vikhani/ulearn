package com.ulearn.catalog.youtube;

/**
 * Resolves a YouTube playlist and its videos using a server-side API key. The key is never exposed
 * to the client (course-catalog spec, "Resolve playlist data through the YouTube Data API").
 */
public interface YouTubeClient {

  /**
   * Resolves a playlist's title and ordered videos.
   *
   * @param playlistId the YouTube playlist identifier
   * @return the playlist's title and ordered videos
   * @throws com.ulearn.catalog.api.PlaylistUnavailableException when the playlist is nonexistent,
   *     private, or unavailable
   * @throws com.ulearn.catalog.api.CatalogUnavailableException when the API is temporarily unusable
   *     (quota exhausted or a transient outage)
   */
  ResolvedPlaylist resolvePlaylist(String playlistId);
}
