package com.ulearn.progress.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, UUID> {

  Optional<LessonProgressEntity> findByUserIdAndPlaylistLessonId(
      UUID userId, UUID playlistLessonId);

  List<LessonProgressEntity> findByUserIdAndPlaylistLessonIdIn(
      UUID userId, Collection<UUID> playlistLessonIds);
}
