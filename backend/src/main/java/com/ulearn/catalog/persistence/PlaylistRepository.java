package com.ulearn.catalog.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<PlaylistEntity, UUID> {

    Optional<PlaylistEntity> findByYoutubePlaylistId(String youtubePlaylistId);
}
