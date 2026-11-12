package com.ulearn.catalog.internal;

import com.ulearn.accounts.api.CurrentUserProvider;
import com.ulearn.accounts.api.OwnershipGuard;
import com.ulearn.catalog.api.CourseCatalog;
import com.ulearn.catalog.api.CourseNotFoundException;
import com.ulearn.catalog.api.CourseView;
import com.ulearn.catalog.api.DuplicateCourseException;
import com.ulearn.catalog.api.LessonView;
import com.ulearn.catalog.persistence.CourseEntity;
import com.ulearn.catalog.persistence.CourseRepository;
import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistLessonEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import com.ulearn.catalog.youtube.PlaylistUrl;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CourseService implements CourseCatalog {

    private final CourseRepository courseRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistSnapshotService playlistSnapshotService;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;

    @Override
    @Transactional
    public CourseView addCourse(String sourceUrl) {
        UUID ownerId = currentUserProvider.currentUser().id();
        String youtubePlaylistId = PlaylistUrl.extractPlaylistId(sourceUrl);
        rejectDuplicate(ownerId, youtubePlaylistId);

        PlaylistEntity playlist = playlistSnapshotService.getOrCreateFresh(youtubePlaylistId);
        CourseEntity course = courseRepository.save(
                new CourseEntity(ownerId, playlist, playlist.getTitle(), sourceUrl));
        return toView(course, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> listCourses() {
        UUID ownerId = currentUserProvider.currentUser().id();
        return courseRepository.findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId).stream()
                .map(course -> toView(course, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getCourse(UUID courseId) {
        return toView(loadOwnedActiveCourse(courseId), true);
    }

    @Override
    @Transactional
    public CourseView renameCourse(UUID courseId, String name) {
        CourseEntity course = loadOwnedActiveCourse(courseId);
        course.rename(name);
        return toView(courseRepository.save(course), true);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId) {
        CourseEntity course = loadOwnedActiveCourse(courseId);
        course.softDelete();
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public CourseView resyncCourse(UUID courseId) {
        CourseEntity course = loadOwnedActiveCourse(courseId);
        playlistSnapshotService.refresh(course.getPlaylist());
        return toView(course, true);
    }

    private void rejectDuplicate(UUID ownerId, String youtubePlaylistId) {
        playlistRepository
                .findByYoutubePlaylistId(youtubePlaylistId)
                .filter(playlist -> courseRepository.existsByOwnerIdAndPlaylistIdAndDeletedAtIsNull(ownerId, playlist.getId()))
                .ifPresent(playlist -> {
                    throw new DuplicateCourseException("This playlist is already in your courses");
                });
    }

    private CourseEntity loadOwnedActiveCourse(UUID courseId) {
        CourseEntity course = courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));
        ownershipGuard.requireOwner(course.getOwnerId());
        return course;
    }

    private static CourseView toView(CourseEntity course, boolean includeLessons) {
        PlaylistEntity playlist = course.getPlaylist();
        int lessonCount = playlist.getLessons().size();
        List<LessonView> lessons = includeLessons
                ? playlist.getLessons().stream().map(CourseService::toLessonView).toList()
                : List.of();
        return new CourseView(
                course.getId(),
                course.getName(),
                course.getSourceUrl(),
                playlist.getYoutubePlaylistId(),
                playlist.getTitle(),
                lessonCount,
                lessonCount > 0,
                course.getCreatedAt(),
                playlist.getLastSyncedAt(),
                lessons);
    }

    private static LessonView toLessonView(PlaylistLessonEntity lesson) {
        return new LessonView(
                lesson.getId(),
                lesson.getPosition(),
                lesson.getTitle(),
                lesson.getVideoId(),
                lesson.getDurationSeconds(),
                lesson.isAvailable(),
                LessonView.youtubeUrl(lesson.getVideoId()));
    }
}
