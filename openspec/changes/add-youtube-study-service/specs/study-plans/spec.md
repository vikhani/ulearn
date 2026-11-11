# Spec Delta

## Purpose

Lets a user set goals and time-bound study plans for their courses and see whether their actual progress is keeping pace with the plan.

## ADDED Requirements

### Requirement: Set a goal for a course

The system SHALL let the owner set a goal for a course, expressed as a target such as completing the course by a date or reaching a percentage, and SHALL allow only one active goal per course.

#### Scenario: Goal created
- **WHEN** the owner sets a goal for a course with a target date or target percentage
- **THEN** the system stores the goal as active for that course

#### Scenario: Replacing an existing goal
- **WHEN** the owner sets a new goal for a course that already has an active goal
- **THEN** the system replaces the previous goal with the new one

#### Scenario: Invalid target rejected
- **WHEN** the owner sets a goal with a target date in the past or a percentage outside 0 to 100
- **THEN** the system rejects the request with a validation error

### Requirement: Create a study plan

The system SHALL let the owner create a study plan for a course specifying a target date and a weekly commitment (for example lessons or minutes per week), and SHALL derive the required pace from the remaining lessons.

#### Scenario: Plan created with derived pace
- **WHEN** the owner creates a plan for a course with remaining lessons, a target date, and a weekly commitment
- **THEN** the system stores the plan and reports the required pace to finish on time

#### Scenario: Target date before start
- **WHEN** the owner creates a plan whose target date precedes the plan start
- **THEN** the system rejects the request with a validation error

### Requirement: Plan versus actual progress

The system SHALL report, for each active plan, the actual progress against the expected progress at the current time, and SHALL indicate whether the user is on track, ahead, or behind.

#### Scenario: Behind schedule reported
- **WHEN** actual completion is less than the expected completion for the elapsed portion of a plan
- **THEN** the system reports the plan as behind schedule with the shortfall

#### Scenario: On track reported
- **WHEN** actual completion meets or exceeds the expected completion for the elapsed portion of a plan
- **THEN** the system reports the plan as on track or ahead

#### Scenario: Plan completed
- **WHEN** the course reaches the plan's target
- **THEN** the system marks the plan complete

### Requirement: Manage plans

The system SHALL let the owner view, update, and delete their study plans.

#### Scenario: Update a plan
- **WHEN** the owner changes a plan's target date or weekly commitment
- **THEN** the system stores the change and recomputes the required pace and on-track status

#### Scenario: Delete a plan
- **WHEN** the owner deletes a plan
- **THEN** the plan no longer appears in the owner's plans and no longer affects reported status

#### Scenario: List own plans
- **WHEN** an authenticated user requests their plans
- **THEN** the system returns only plans for courses they own
