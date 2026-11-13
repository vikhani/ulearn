package com.ulearn.progress.web;

import com.ulearn.progress.api.LessonNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the progress contract's exceptions to the standard {@code application/problem+json} shape.
 */
@RestControllerAdvice
public class ProgressExceptionHandler {

  @ExceptionHandler(LessonNotFoundException.class)
  ProblemDetail lessonNotFound(LessonNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Lesson not found");
    return problem;
  }
}
