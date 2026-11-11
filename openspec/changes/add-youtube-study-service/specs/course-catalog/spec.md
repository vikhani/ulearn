# Spec Delta

## Purpose

Lets a user turn a YouTube playlist into a structured course by supplying its URL, resolving the playlist and its videos through the YouTube Data API, and keeping that snapshot current.

## ADDED Requirements

### Requirement: Add a playlist as a course by URL

The system SHALL let an authenticated user add a course by submitting a YouTube playlist URL, and SHALL persist the resulting course as owned by that user.

#### Scenario: Valid playlist URL accepted
- **WHEN** an authenticated user submits a valid, publicly accessible YouTube playlist URL
- **THEN** the system creates a course owned by that user containing the playlist's title, source URL, and ordered lessons

#### Scenario: Non-playlist URL rejected
- **WHEN** a user submits a URL that is not a YouTube playlist URL (for example a single video or an unrelated site)
- **THEN** the system rejects the request with a validation error and creates no course

#### Scenario: Duplicate course for same playlist
- **WHEN** a user submits a playlist URL they already added
- **THEN** the system informs the user the playlist is already in their courses and does not create a second course

### Requirement: Resolve playlist data through the YouTube Data API

The system SHALL resolve playlist and video metadata using a server-side YouTube Data API key, and MUST NOT require the user to supply an API key.

#### Scenario: Playlist resolved server-side
- **WHEN** a course is created or re-synced
- **THEN** the system calls the YouTube Data API using the configured server-side key and never exposes that key to the client

#### Scenario: Playlist not found or unavailable
- **WHEN** the YouTube Data API reports the playlist as nonexistent, private, or unavailable
- **THEN** the system rejects the request with an explanatory error and creates or modifies no course

#### Scenario: API quota or outage
- **WHEN** the YouTube Data API call fails because of quota exhaustion or a transient outage
- **THEN** the system returns a retriable error without partially creating or corrupting the course

### Requirement: Snapshot ordered lessons

The system SHALL store each playlist video as a lesson with its position, title, video identifier, and duration, preserving the playlist's order.

#### Scenario: Lessons captured in order
- **WHEN** a course is created from a playlist with N videos
- **THEN** the course contains N lessons whose positions reflect the playlist order and whose titles and durations come from the YouTube Data API

#### Scenario: Empty playlist
- **WHEN** a playlist resolves with zero videos
- **THEN** the system creates the course with no lessons and marks it as having no lessons

### Requirement: Re-sync course content

The system SHALL let the owner re-sync a course so that added, removed, or reordered videos in the source playlist are reflected, without losing the user's existing progress for lessons that remain.

#### Scenario: New videos appear after re-sync
- **WHEN** the owner re-syncs a course whose source playlist has gained videos
- **THEN** the new videos appear as lessons and existing lessons keep their recorded progress

#### Scenario: Removed videos are marked unavailable
- **WHEN** the owner re-syncs a course and a previously present video is no longer in the playlist
- **THEN** the lesson is retained and marked unavailable, keeping its stored metadata and a reference to the YouTube page, while the user's progress on the remaining lessons is preserved

#### Scenario: Stale playlists refreshed on a schedule
- **WHEN** a playlist snapshot is not fresh and the weekly refresh runs
- **THEN** the snapshot's lessons are updated and affected courses' progress is recomputed

### Requirement: Manage owned courses

The system SHALL let the owner list, view, rename, and delete their courses.

#### Scenario: List own courses
- **WHEN** an authenticated user requests their course list
- **THEN** the system returns only the courses they own

#### Scenario: Delete a course
- **WHEN** the owner deletes a course
- **THEN** the course and its lessons are removed and no longer appear in the owner's list
