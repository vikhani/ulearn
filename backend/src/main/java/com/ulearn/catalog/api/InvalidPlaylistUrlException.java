package com.ulearn.catalog.api;

/** Raised when a submitted URL is not a usable YouTube playlist URL. */
public class InvalidPlaylistUrlException extends RuntimeException {

    public InvalidPlaylistUrlException(String message) {
        super(message);
    }
}
