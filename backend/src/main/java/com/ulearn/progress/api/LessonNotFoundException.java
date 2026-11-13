package com.ulearn.progress.api;

/** Raised when a Lesson does not exist within the owner's Course. */
public class LessonNotFoundException extends RuntimeException {

  public LessonNotFoundException(String message) {
    super(message);
  }
}
