package com.ulearn.catalog.api;

import java.util.List;
import java.util.UUID;

/**
 * The catalog capability's contract, scoped to the authenticated owner. Implementations derive the
 * owner from the authenticated principal (never from client input) and enforce ownership on every
 * course-specific operation (ADR-0008).
 */
public interface CourseCatalog {

    /** Adds a Course from a YouTube playlist URL, reusing an existing Playlist snapshot when possible. */
    CourseView addCourse(String sourceUrl);

    /** Lists the authenticated owner's active Courses, newest first. */
    List<CourseView> listCourses();

    /** Loads one owned Course with its ordered lessons. */
    CourseView getCourse(UUID courseId);

    /** Renames an owned Course. */
    CourseView renameCourse(UUID courseId, String name);

    /** Soft-deletes an owned Course; it is hidden but its references stay intact. */
    void deleteCourse(UUID courseId);

    /** Re-syncs an owned Course against its source playlist. */
    CourseView resyncCourse(UUID courseId);
}
