package com.ulearn.catalog.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ulearn.catalog.api.CatalogUnavailableException;
import com.ulearn.catalog.api.PlaylistUnavailableException;
import com.ulearn.config.YouTubeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YouTubeDataApiClientTest {

  private static final String BASE = "https://www.googleapis.com/youtube/v3";

  private MockRestServiceServer server;
  private YouTubeDataApiClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    YouTubeProperties properties = new YouTubeProperties();
    properties.setApiKey("test-key");
    client = new YouTubeDataApiClient(builder, properties);
  }

  @Test
  void resolvesPlaylistMetadataVideosAndDurations() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andExpect(queryParam("id", "PL123"))
        .andRespond(
            withSuccess(
                """
                {"items":[{"snippet":{"title":"My Playlist"}}]}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(startsWith(BASE + "/playlistItems")))
        .andExpect(queryParam("playlistId", "PL123"))
        .andRespond(
            withSuccess(
                """
                {"items":[
                  {"snippet":{"title":"First","position":0,"resourceId":{"videoId":"vid1"}}},
                  {"snippet":{"title":"Second","position":1,"resourceId":{"videoId":"vid2"}}}
                ]}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(startsWith(BASE + "/videos")))
        .andExpect(queryParam("part", "contentDetails"))
        .andRespond(
            withSuccess(
                """
                {"items":[
                  {"id":"vid1","contentDetails":{"duration":"PT1M30S"}},
                  {"id":"vid2","contentDetails":{"duration":"PT2H5M"}}
                ]}
                """,
                MediaType.APPLICATION_JSON));

    ResolvedPlaylist playlist = client.resolvePlaylist("PL123");

    assertThat(playlist.playlistId()).isEqualTo("PL123");
    assertThat(playlist.title()).isEqualTo("My Playlist");
    assertThat(playlist.videos())
        .extracting(ResolvedVideo::videoId)
        .containsExactly("vid1", "vid2");
    assertThat(playlist.videos()).extracting(ResolvedVideo::position).containsExactly(0, 1);
    assertThat(playlist.videos())
        .extracting(ResolvedVideo::durationSeconds)
        .containsExactly(90, 7500);
    server.verify();
  }

  @Test
  void followsPlaylistItemPagination() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(
            withSuccess(
                "{\"items\":[{\"snippet\":{\"title\":\"Paged\"}}]}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(startsWith(BASE + "/playlistItems")))
        .andExpect(queryParam("playlistId", "PL123"))
        .andRespond(
            withSuccess(
                """
                {"items":[{"snippet":{"title":"First","resourceId":{"videoId":"vid1"}}}],
                 "nextPageToken":"token-2"}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(startsWith(BASE + "/playlistItems")))
        .andExpect(queryParam("pageToken", "token-2"))
        .andRespond(
            withSuccess(
                """
                {"items":[{"snippet":{"title":"Second","resourceId":{"videoId":"vid2"}}}]}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(startsWith(BASE + "/videos")))
        .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

    ResolvedPlaylist playlist = client.resolvePlaylist("PL123");

    assertThat(playlist.videos())
        .extracting(ResolvedVideo::videoId)
        .containsExactly("vid1", "vid2");
    assertThat(playlist.videos()).extracting(ResolvedVideo::position).containsExactly(0, 1);
    server.verify();
  }

  @Test
  void mapsNotFoundToPlaylistUnavailable() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> client.resolvePlaylist("PL123"))
        .isInstanceOf(PlaylistUnavailableException.class);
  }

  @Test
  void mapsPrivatePlaylistToUnavailable() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(
            withStatus(HttpStatus.FORBIDDEN)
                .body("{\"error\":{\"errors\":[{\"reason\":\"forbidden\"}]}}")
                .contentType(MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.resolvePlaylist("PL123"))
        .isInstanceOf(PlaylistUnavailableException.class);
  }

  @Test
  void mapsQuotaExhaustionToRetriableError() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(
            withStatus(HttpStatus.FORBIDDEN)
                .body("{\"error\":{\"errors\":[{\"reason\":\"quotaExceeded\"}]}}")
                .contentType(MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.resolvePlaylist("PL123"))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void mapsServerErrorToRetriableError() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> client.resolvePlaylist("PL123"))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void treatsMissingPlaylistItemsAsUnavailable() {
    server
        .expect(requestTo(startsWith(BASE + "/playlists")))
        .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.resolvePlaylist("PL123"))
        .isInstanceOf(PlaylistUnavailableException.class);
  }
}
