package com.ulearn.catalog.api;

/**
 * Raised when the YouTube Data API is temporarily unusable (quota exhausted, transient outage). Callers
 * can retry; no partial Course is created or corrupted.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
        super(message);
    }
}
