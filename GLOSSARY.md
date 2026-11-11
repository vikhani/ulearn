# Glossary

Canonical vocabulary for this project. OpenSpec artifacts, issues, tests, and code use these terms with these meanings; don't drift to synonyms the glossary avoids.

## Domain terms

### Course
A user-owned study unit built from a YouTube playlist. It references a global Playlist snapshot and owns the user's Goal, Study Plan, progress, visibility, and name. Two users who add the same playlist get two Courses sharing one Playlist snapshot. See ADR-0005.

### Playlist (snapshot)
The centrally stored, shared snapshot of a YouTube playlist and its videos, refreshed on a schedule. Users never see it directly, progress is never attached to it, and it exists only while a Course references it. Distinct from a Course.

### Lesson
A single video belonging to a Playlist, surfaced within a Course, with a position, title, video id, duration, and availability state.

### Available lesson
A Lesson whose video is currently present in the source playlist.

### Unavailable lesson
A Lesson whose video is no longer in the source playlist. Retained with its stored title and a reference to the YouTube page. It **counts** in progress but cannot be tracked automatically — only marked complete manually. See ADR-0004.

### Progress
A user's completion state for a Lesson, tracked automatically while the user watches the Lesson inside ulearn's embedded player, and adjustable manually. **Course progress** = completed lessons ÷ all lessons in the playlist (available and unavailable). It is **undefined** (reported as `null`) only when the playlist is empty (0/0). See ADR-0001, ADR-0004.

### Goal
A desired outcome for a Course: a target date or a target percentage. At most one active Goal per Course.

### Study Plan
A schedule for a Course: a target date plus a weekly commitment, from which a required pace is derived, plus an on-track / ahead / behind status. Distinct from a Goal. See ADR-0003.

### Compilation
An ordered bundle of a user's Courses, used to organize and share a larger curriculum. Its progress is weighted by lesson count across member Courses. Deleting a Compilation does not delete its member Courses. See ADR-0007.

### Share link
An unguessable, revocable URL that exposes a Course or Compilation's contents read-only. Exposes structure only — never progress — and grants no ability to import or copy. See ADR-0002.

### Owner
The user who owns a resource (Course, Compilation, Goal, Study Plan, Share link). All access is scoped to the Owner.

## Avoid

- **"Playlist" for Course** — a playlist is the global YouTube snapshot; a Course is the user's entity built from it.
- **"Watch history" for progress** — progress comes from ulearn's embedded player, not from YouTube watch history (see ADR-0001).
- **"Import" / "copy" of a shared item** — explicitly out of scope (see ADR-0002).
- **"Available lessons" as the progress denominator** — the denominator is all lessons; unavailable ones still count (see ADR-0004).
