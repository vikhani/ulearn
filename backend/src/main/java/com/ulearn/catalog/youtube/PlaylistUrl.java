package com.ulearn.catalog.youtube;

import com.ulearn.catalog.api.InvalidPlaylistUrlException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts a YouTube playlist identifier from a user-supplied URL. Only YouTube playlist URLs are
 * accepted: a single-video link (no {@code list} parameter) or an unrelated site is rejected
 * (course-catalog spec, "Non-playlist URL rejected").
 */
public final class PlaylistUrl {

  private static final Set<String> YOUTUBE_HOSTS =
      Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com");
  private static final Pattern PLAYLIST_ID = Pattern.compile("^[A-Za-z0-9_-]{10,64}$");

  private PlaylistUrl() {}

  public static String extractPlaylistId(String url) {
    if (url == null || url.isBlank()) {
      throw new InvalidPlaylistUrlException("A YouTube playlist URL is required");
    }
    URI uri;
    try {
      uri = new URI(url.trim());
    } catch (URISyntaxException ex) {
      throw new InvalidPlaylistUrlException("Not a valid URL");
    }
    String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase();
    if (!YOUTUBE_HOSTS.contains(host)) {
      throw new InvalidPlaylistUrlException("Only YouTube playlist URLs are supported");
    }
    String playlistId = queryParam(uri.getRawQuery(), "list");
    if (playlistId == null || !PLAYLIST_ID.matcher(playlistId).matches()) {
      throw new InvalidPlaylistUrlException("The URL does not point to a YouTube playlist");
    }
    return playlistId;
  }

  private static String queryParam(String rawQuery, String name) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return null;
    }
    return Arrays.stream(rawQuery.split("&"))
        .map(part -> part.split("=", 2))
        .filter(pair -> pair.length == 2 && pair[0].equals(name))
        .map(pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8))
        .findFirst()
        .orElse(null);
  }
}
