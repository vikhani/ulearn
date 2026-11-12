package com.ulearn.catalog.web;

import com.ulearn.catalog.api.CatalogUnavailableException;
import com.ulearn.catalog.api.CourseNotFoundException;
import com.ulearn.catalog.api.DuplicateCourseException;
import com.ulearn.catalog.api.InvalidPlaylistUrlException;
import com.ulearn.catalog.api.PlaylistUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the catalog contract's exceptions to the standard {@code application/problem+json} shape.
 */
@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(InvalidPlaylistUrlException.class)
    ProblemDetail invalidPlaylistUrl(InvalidPlaylistUrlException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid playlist URL", ex.getMessage());
    }

    @ExceptionHandler(PlaylistUnavailableException.class)
    ProblemDetail playlistUnavailable(PlaylistUnavailableException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Playlist unavailable", ex.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail catalogUnavailable(CatalogUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "YouTube temporarily unavailable", ex.getMessage());
    }

    @ExceptionHandler(DuplicateCourseException.class)
    ProblemDetail duplicateCourse(DuplicateCourseException ex) {
        return problem(HttpStatus.CONFLICT, "Duplicate course", ex.getMessage());
    }

    @ExceptionHandler(CourseNotFoundException.class)
    ProblemDetail courseNotFound(CourseNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Course not found", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
