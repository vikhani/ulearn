# Spec Delta

## Purpose

Lets a user group several of their courses into an ordered compilation, so a larger curriculum of playlists can be organized, studied, and shared as one unit.

## ADDED Requirements

### Requirement: Create a compilation

The system SHALL let an authenticated user create a compilation, and SHALL allow only courses the user owns to be included.

#### Scenario: Compilation created
- **WHEN** a user creates a compilation with a title
- **THEN** the system stores the compilation as owned by that user

#### Scenario: Missing title rejected
- **WHEN** a user creates a compilation without a title
- **THEN** the system rejects the request with a validation error

### Requirement: Manage compilation contents

The system SHALL let the owner add, remove, and reorder courses within a compilation, and SHALL prevent the same course from appearing more than once in the same compilation.

#### Scenario: Course added to compilation
- **WHEN** the owner adds a course they own to a compilation
- **THEN** the compilation includes that course

#### Scenario: Course the user does not own rejected
- **WHEN** the owner attempts to add a course owned by another user to a compilation
- **THEN** the system rejects the request and leaves the compilation unchanged

#### Scenario: Duplicate course rejected
- **WHEN** the owner attempts to add a course already present in the compilation
- **THEN** the system rejects the request and leaves the compilation unchanged

#### Scenario: Reorder courses
- **WHEN** the owner changes the order of courses in a compilation
- **THEN** the compilation reflects the new order

### Requirement: Compilation progress

The system SHALL report a compilation's progress as completed lessons divided by total lessons across all member courses, weighting larger courses proportionally.

#### Scenario: Weighted aggregate progress reported
- **WHEN** a user views a compilation whose member courses have recorded progress
- **THEN** the system reports progress as completed lessons ÷ total lessons across all member courses

#### Scenario: Empty compilation progress
- **WHEN** a compilation contains no courses, or its member courses have no lessons
- **THEN** the system reports progress as undefined (null) rather than 100 percent

### Requirement: Manage compilations

The system SHALL let the owner list, view, rename, and delete their compilations.

#### Scenario: List own compilations
- **WHEN** an authenticated user requests their compilations
- **THEN** the system returns only compilations they own

#### Scenario: Delete a compilation
- **WHEN** the owner deletes a compilation
- **THEN** the compilation is removed without deleting its member courses
