package com.ulearn.catalog.web;

import com.ulearn.catalog.api.CourseCatalog;
import com.ulearn.catalog.api.CourseView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Course endpoints for the authenticated owner. Requests carry no user id: the catalog derives the
 * owner from the authenticated principal (ADR-0008).
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

  private final CourseCatalog courseCatalog;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  CourseView add(@Valid @RequestBody AddCourseRequest request) {
    return courseCatalog.addCourse(request.url());
  }

  @GetMapping
  List<CourseView> list() {
    return courseCatalog.listCourses();
  }

  @GetMapping("/{courseId}")
  CourseView get(@PathVariable UUID courseId) {
    return courseCatalog.getCourse(courseId);
  }

  @PatchMapping("/{courseId}")
  CourseView rename(@PathVariable UUID courseId, @Valid @RequestBody RenameCourseRequest request) {
    return courseCatalog.renameCourse(courseId, request.name());
  }

  @DeleteMapping("/{courseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID courseId) {
    courseCatalog.deleteCourse(courseId);
  }

  @PostMapping("/{courseId}/resync")
  CourseView resync(@PathVariable UUID courseId) {
    return courseCatalog.resyncCourse(courseId);
  }

  public record AddCourseRequest(@NotBlank(message = "url is required") String url) {}

  public record RenameCourseRequest(
      @NotBlank(message = "name is required") @Size(max = 200) String name) {}
}
