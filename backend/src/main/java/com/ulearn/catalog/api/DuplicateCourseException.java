package com.ulearn.catalog.api;

/** Raised when a user tries to add a playlist that is already one of their active courses. */
public class DuplicateCourseException extends RuntimeException {

    public DuplicateCourseException(String message) {
        super(message);
    }
}
