# Spec Delta

## Purpose

Derives each user's per-lesson and per-course study progress by tracking playback inside ulearn's embedded player, so learners can see how far they have progressed without manual bookkeeping.

## ADDED Requirements

### Requirement: Automatic progress tracking in the embedded player

The system SHALL track lesson progress automatically while the user watches a lesson inside ulearn, using the embedded YouTube player to observe playback state and elapsed time, and SHALL mark the lesson complete when watched time reaches a configured threshold of the lesson's duration.

#### Scenario: Lesson watched to completion
- **WHEN** a user watches a lesson in ulearn to at least the completion threshold of its duration
- **THEN** the system records that lesson as complete for that user

#### Scenario: Partially watched lesson recorded
- **WHEN** a user watches part of a lesson in ulearn below the completion threshold
- **THEN** the system records the lesson as in progress rather than complete

#### Scenario: Watching outside ulearn is not detected
- **WHEN** a user watches a lesson's video directly on YouTube rather than in ulearn
- **THEN** the system records no progress from that playback and the user can adjust progress manually

#### Scenario: Repeated progress reports are idempotent
- **WHEN** the same playback progress is reported to the system more than once
- **THEN** the user's recorded progress is unchanged by the repeat report

### Requirement: Manual progress adjustment

The system SHALL let the user mark a lesson complete or not complete manually, and a manual mark SHALL take precedence over automatic tracking for that lesson until the user clears it.

#### Scenario: User marks a lesson complete
- **WHEN** a user manually marks a lesson complete
- **THEN** the lesson is recorded as complete and subsequent automatic tracking does not override the manual mark

#### Scenario: Unavailable lesson manually completed
- **WHEN** a user manually marks an unavailable lesson complete
- **THEN** the lesson is recorded as complete and counts toward course progress

#### Scenario: User clears a manual mark
- **WHEN** a user clears a previously set manual mark
- **THEN** the lesson returns to being governed by automatic tracking

### Requirement: Course progress computation

The system SHALL compute course progress as completed lessons divided by all lessons in the course, and SHALL recompute it whenever a lesson's state or the lesson set changes.

#### Scenario: Progress reflects completed lessons
- **WHEN** a course has N lessons (available and unavailable) and C of them are complete
- **THEN** the reported course progress equals C divided by N as a percentage

#### Scenario: Unavailable lessons count toward progress
- **WHEN** a course contains an unavailable lesson that the user has marked complete
- **THEN** the lesson counts in both the completed and the total counts

#### Scenario: Re-sync preserves computed progress
- **WHEN** a course is re-synced and its lesson set changes
- **THEN** course progress is recomputed against the new lesson set

#### Scenario: Empty playlist progress is undefined
- **WHEN** a course's playlist is empty
- **THEN** the system reports progress as undefined (null) rather than 0 percent or 100 percent

### Requirement: Course completion state

The system SHALL mark a course complete only when every lesson is complete, and SHALL return it to incomplete if a re-sync adds new incomplete lessons.

#### Scenario: All lessons complete
- **WHEN** every lesson of a course is complete
- **THEN** the system marks the course complete

#### Scenario: Completion revoked by new lessons
- **WHEN** a completed course gains a new, incomplete lesson during re-sync
- **THEN** the system marks the course incomplete again

### Requirement: Progress is per user

The system SHALL keep each user's progress separate so that one user's activity never changes another user's recorded progress.

#### Scenario: Progress is isolated between users
- **WHEN** two users study the same course
- **THEN** each user sees only their own lesson and course progress
