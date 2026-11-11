# ADR-0007: Compilation progress is weighted by lessons

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

"Aggregate" progress for a Compilation was undefined, and member Courses can differ greatly in size.

## Decision

Compute Compilation progress as completed lessons ÷ total lessons across all member Courses (available and unavailable). Large Courses count proportionally. Progress is undefined (`null`) when the Compilation has no lessons (no members, or every member's playlist is empty).

## Consequences

- A Compilation's progress is not the mean of member percentages.
- Empty Compilations report undefined, not 100%.
- Reuses the per-Course lesson counts, so it stays consistent with Course progress.
