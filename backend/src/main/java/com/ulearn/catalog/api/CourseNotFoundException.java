package com.ulearn.catalog.api;

/** Raised when a Course does not exist or has been soft-deleted. */
public class CourseNotFoundException extends RuntimeException {

  public CourseNotFoundException(String message) {
    super(message);
  }
}
