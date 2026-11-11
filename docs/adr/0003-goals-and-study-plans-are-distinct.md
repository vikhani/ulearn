# ADR-0003: Goals and Study Plans are distinct

- **Status**: Accepted
- **Date**: 2026-11-11

## Context

The `study-plans` capability described both "goals" and "study plans" attached to a Course, each carrying a target date/percentage. Their relationship was ambiguous, risking a redundant data model.

## Decision

Model them as two distinct concepts. A **Goal** is a desired outcome (target date or target percentage); at most one active Goal per Course. A **Study Plan** is a schedule (target date + weekly commitment) that derives a required pace and an on-track / behind status.

## Consequences

- Two entities and two requirement sets, both owned by the `study-plans` capability.
- UI presents them as related but distinct.
- Glossary defines each precisely to prevent drift.
