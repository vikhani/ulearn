package com.ulearn.catalog.internal;

import com.ulearn.catalog.persistence.PlaylistEntity;
import com.ulearn.catalog.persistence.PlaylistLessonEntity;
import com.ulearn.catalog.persistence.PlaylistRepository;
import com.ulearn.catalog.youtube.ResolvedPlaylist;
import com.ulearn.catalog.youtube.ResolvedVideo;
import com.ulearn.catalog.youtube.YouTubeClient;
import com.ulearn.config.CatalogProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the global Playlist snapshot lifecycle: reuse a fresh snapshot without touching the API, refresh
 * a stale one, and fold resolved videos into the stored lessons. Re-syncs never delete a Lesson — a
 * video that disappears is marked unavailable so its row (and any progress keyed to it) survives
 * (ADR-0005, ADR-0004, ADR-0009).
 */
@Component
@RequiredArgsConstructor
class PlaylistSnapshotService {

    private final PlaylistRepository playlistRepository;
    private final YouTubeClient youTubeClient;
    private final CatalogProperties catalogProperties;

    /** Returns a fresh snapshot, resolving it through the API only when absent or stale. */
    @Transactional
    public PlaylistEntity getOrCreateFresh(String youtubePlaylistId) {
        PlaylistEntity playlist =
                playlistRepository.findByYoutubePlaylistId(youtubePlaylistId).orElse(null);
        if (playlist != null && isFresh(playlist)) {
            return playlist;
        }
        ResolvedPlaylist resolved = youTubeClient.resolvePlaylist(youtubePlaylistId);
        if (playlist == null) {
            playlist = new PlaylistEntity(youtubePlaylistId, resolved.title());
        }
        apply(playlist, resolved);
        return playlistRepository.save(playlist);
    }

    /** Refreshes a snapshot in place, preserving existing lessons and their progress. */
    @Transactional
    public PlaylistEntity refresh(PlaylistEntity playlist) {
        ResolvedPlaylist resolved = youTubeClient.resolvePlaylist(playlist.getYoutubePlaylistId());
        apply(playlist, resolved);
        return playlistRepository.save(playlist);
    }

    private boolean isFresh(PlaylistEntity playlist) {
        Duration threshold = catalogProperties.getFreshnessThreshold();
        return playlist.getLastSyncedAt().isAfter(Instant.now().minus(threshold));
    }

    private void apply(PlaylistEntity playlist, ResolvedPlaylist resolved) {
        playlist.rename(resolved.title());
        playlist.markSynced(Instant.now());

        Map<String, PlaylistLessonEntity> existing = playlist.getLessons().stream()
                .collect(Collectors.toMap(PlaylistLessonEntity::getVideoId, Function.identity(), (first, ignored) -> first));
        Set<String> presentVideoIds = new HashSet<>();
        for (ResolvedVideo video : resolved.videos()) {
            presentVideoIds.add(video.videoId());
            PlaylistLessonEntity lesson = existing.get(video.videoId());
            if (lesson == null) {
                playlist.addLesson(video.videoId(), video.title(), video.position(), video.durationSeconds());
            } else {
                lesson.update(video.title(), video.position(), video.durationSeconds());
            }
        }
        playlist.getLessons().stream()
                .filter(lesson -> !presentVideoIds.contains(lesson.getVideoId()))
                .forEach(PlaylistLessonEntity::markUnavailable);
    }
}
