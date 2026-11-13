package com.ulearn.catalog.api;

/** Raised when the YouTube Data API reports a playlist as nonexistent, private, or otherwise unavailable. */
public class PlaylistUnavailableException extends RuntimeException {

    public PlaylistUnavailableException(String message) {
        super(message);
    }
}
