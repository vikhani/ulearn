package com.ulearn.catalog.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ulearn.catalog.api.CatalogUnavailableException;
import com.ulearn.catalog.api.PlaylistUnavailableException;
import com.ulearn.config.YouTubeProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Resolves playlists through the YouTube Data API v3 using the server-side key. Playlist items are
 * paged (50 per request) and video durations are fetched in batches of up to 50 ids, so resolving a
 * large playlist stays within a predictable number of quota units (ADR-0005, ADR-0009). API
 * failures are mapped to the catalog contract's exceptions so callers can distinguish a missing
 * playlist from a retriable outage.
 */
@Component
public class YouTubeDataApiClient implements YouTubeClient {

  private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";
  private static final int PAGE_SIZE = 50;
  private static final int ID_BATCH_SIZE = 50;
  private static final String QUOTA_REASON = "quotaExceeded";

  private final RestClient restClient;
  private final String apiKey;

  public YouTubeDataApiClient(RestClient.Builder restClientBuilder, YouTubeProperties properties) {
    this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    this.apiKey = properties.getApiKey();
  }

  @Override
  public ResolvedPlaylist resolvePlaylist(String playlistId) {
    PlaylistListResponse playlists = fetchPlaylists(playlistId);
    if (playlists.items() == null || playlists.items().isEmpty()) {
      throw new PlaylistUnavailableException("Playlist " + playlistId + " is not available");
    }
    String title = playlists.items().get(0).snippet().title();

    List<RawVideo> rawVideos = fetchPlaylistItems(playlistId);
    Map<String, Integer> durations =
        fetchDurations(rawVideos.stream().map(RawVideo::videoId).toList());

    List<ResolvedVideo> videos =
        rawVideos.stream()
            .map(raw ->
                new ResolvedVideo(
                    raw.videoId(),
                    raw.title(),
                    raw.position(),
                    durations.getOrDefault(raw.videoId(), 0)))
            .toList();
    return new ResolvedPlaylist(playlistId, title, videos);
  }

  private PlaylistListResponse fetchPlaylists(String playlistId) {
    return execute(
        restClient
            .get()
            .uri(builder ->
                builder
                    .path("/playlists")
                    .queryParam("part", "snippet")
                    .queryParam("id", playlistId)
                    .queryParam("key", apiKey)
                    .build()),
        PlaylistListResponse.class);
  }

  private List<RawVideo> fetchPlaylistItems(String playlistId) {
    List<RawVideo> videos = new ArrayList<>();
    String pageToken = null;
    do {
      String token = pageToken;
      PlaylistItemsResponse response =
          execute(
              restClient
                  .get()
                  .uri(builder -> {
                    builder
                        .path("/playlistItems")
                        .queryParam("part", "snippet")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", PAGE_SIZE)
                        .queryParam("key", apiKey);
                    if (token != null) {
                      builder.queryParam("pageToken", token);
                    }
                    return builder.build();
                  }),
              PlaylistItemsResponse.class);
      List<PlaylistItemsResponse.PlaylistItem> items =
          response.items() == null ? List.of() : response.items();
      for (int index = 0; index < items.size(); index++) {
        PlaylistItemsResponse.PlaylistItem item = items.get(index);
        Integer position = item.snippet().position();
        videos.add(
            new RawVideo(
                item.snippet().resourceId().videoId(),
                item.snippet().title(),
                position == null ? videos.size() : position));
      }
      pageToken = response.nextPageToken();
    } while (pageToken != null);
    return videos;
  }

  private Map<String, Integer> fetchDurations(List<String> videoIds) {
    Map<String, Integer> durations = new HashMap<>();
    for (int start = 0; start < videoIds.size(); start += ID_BATCH_SIZE) {
      List<String> batch =
          videoIds.subList(start, Math.min(start + ID_BATCH_SIZE, videoIds.size()));
      VideoListResponse response =
          execute(
              restClient
                  .get()
                  .uri(builder ->
                      builder
                          .path("/videos")
                          .queryParam("part", "contentDetails")
                          .queryParam("id", String.join(",", batch))
                          .queryParam("key", apiKey)
                          .build()),
              VideoListResponse.class);
      if (response.items() != null) {
        for (VideoListResponse.VideoItem item : response.items()) {
          ContentDetails details = item.contentDetails();
          durations.put(
              item.id(), parseDurationSeconds(details == null ? null : details.duration()));
        }
      }
    }
    return durations;
  }

  private static int parseDurationSeconds(String isoDuration) {
    if (isoDuration == null || isoDuration.isBlank()) {
      return 0;
    }
    try {
      return (int) Duration.parse(isoDuration).getSeconds();
    } catch (RuntimeException ex) {
      return 0;
    }
  }

  private <T> T execute(RestClient.RequestHeadersSpec<?> request, Class<T> responseType) {
    try {
      return request.retrieve().body(responseType);
    } catch (HttpClientErrorException.NotFound ex) {
      throw new PlaylistUnavailableException("Playlist is not available");
    } catch (HttpClientErrorException.Forbidden ex) {
      if (ex.getResponseBodyAsString().contains(QUOTA_REASON)) {
        throw new CatalogUnavailableException("YouTube Data API quota exceeded");
      }
      throw new PlaylistUnavailableException("Playlist is private or unavailable");
    } catch (HttpClientErrorException ex) {
      throw new PlaylistUnavailableException(
          "Playlist could not be resolved (" + ex.getStatusCode() + ")");
    } catch (HttpServerErrorException ex) {
      throw new CatalogUnavailableException("YouTube Data API is temporarily unavailable");
    } catch (ResourceAccessException ex) {
      throw new CatalogUnavailableException("YouTube Data API is unreachable");
    }
  }

  private record RawVideo(String videoId, String title, int position) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PlaylistListResponse(List<PlaylistItem> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaylistItem(Snippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Snippet(String title) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PlaylistItemsResponse(List<PlaylistItem> items, String nextPageToken) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaylistItem(Snippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Snippet(String title, Integer position, ResourceId resourceId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResourceId(String videoId) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record VideoListResponse(List<VideoItem> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VideoItem(String id, ContentDetails contentDetails) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentDetails(String duration) {}
}
