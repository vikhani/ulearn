package com.ulearn.catalog.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ulearn.catalog.api.InvalidPlaylistUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaylistUrlTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://www.youtube.com/playlist?list=PLabcdefghijklmnop",
                "https://youtube.com/playlist?list=PLabcdefghijklmnop&si=abc",
                "https://m.youtube.com/playlist?list=PLabcdefghijklmnop",
                "https://music.youtube.com/playlist?list=PLabcdefghijklmnop"
            })
    void extractsPlaylistIdFromPlaylistUrls(String url) {
        assertThat(PlaylistUrl.extractPlaylistId(url)).isEqualTo("PLabcdefghijklmnop");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://example.com/playlist?list=PLabcdefghijklmnop",
                "https://www.youtube.com/playlist",
                "https://www.youtube.com/playlist?list=short",
                "not a url",
                " ",
                ""
            })
    void rejectsNonPlaylistOrInvalidUrls(String url) {
        assertThatThrownBy(() -> PlaylistUrl.extractPlaylistId(url))
                .isInstanceOf(InvalidPlaylistUrlException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> PlaylistUrl.extractPlaylistId(null))
                .isInstanceOf(InvalidPlaylistUrlException.class);
    }
}
