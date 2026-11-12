package com.ulearn.catalog.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {

    List<CourseEntity> findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

    Optional<CourseEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOwnerIdAndPlaylistIdAndDeletedAtIsNull(UUID ownerId, UUID playlistId);
}
