package com.ulearn.progress.web;

import com.ulearn.progress.api.CourseProgressView;
import com.ulearn.progress.api.LessonProgressView;
import com.ulearn.progress.api.ProgressTracking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes progress for the authenticated owner's Courses. The player posts heartbeats while a
 * Lesson plays; the user can mark or clear a Lesson manually. Course-scoped paths let the progress
 * capability validate the Lesson against an owned Course.
 */
@RestController
@RequestMapping("/api/courses/{courseId}")
@RequiredArgsConstructor
public class ProgressController {

  private final ProgressTracking progressTracking;

  @GetMapping("/progress")
  CourseProgressView courseProgress(@PathVariable UUID courseId) {
    return progressTracking.getCourseProgress(courseId);
  }

  @PostMapping("/lessons/{lessonId}/heartbeat")
  LessonProgressView heartbeat(
      @PathVariable UUID courseId,
      @PathVariable UUID lessonId,
      @Valid @RequestBody HeartbeatRequest request) {
    return progressTracking.recordHeartbeat(courseId, lessonId, request.watchedSeconds());
  }

  @PutMapping("/lessons/{lessonId}/progress")
  LessonProgressView markLesson(
      @PathVariable UUID courseId,
      @PathVariable UUID lessonId,
      @Valid @RequestBody MarkLessonRequest request) {
    return progressTracking.markLesson(courseId, lessonId, request.completed());
  }

  @DeleteMapping("/lessons/{lessonId}/progress")
  LessonProgressView clearLessonMark(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
    return progressTracking.clearLessonMark(courseId, lessonId);
  }

  public record HeartbeatRequest(
      @Min(value = 0, message = "watchedSeconds must not be negative") int watchedSeconds) {}

  public record MarkLessonRequest(@NotNull(message = "completed is required") Boolean completed) {}
}
