# Spec Delta

## Purpose

Provides account registration, authentication, and profile management so that every course, plan, compilation, and share link is owned by an identified user.

## ADDED Requirements

### Requirement: User registration

The system SHALL allow a visitor to create an account with an email address and password, and SHALL reject registration when the email is already in use.

#### Scenario: Successful registration
- **WHEN** a visitor submits a valid, unused email and a password meeting the password policy
- **THEN** the system creates the account and returns an authenticated session for the new user

#### Scenario: Duplicate email rejected
- **WHEN** a visitor submits an email that already belongs to an account
- **THEN** the system rejects the request with a conflict error and creates no account

#### Scenario: Weak password rejected
- **WHEN** a visitor submits a password that does not meet the password policy
- **THEN** the system rejects the request with a validation error describing the unmet policy

### Requirement: User login and session lifecycle

The system SHALL authenticate users by email and password and SHALL issue a session token that expires after a configured lifetime.

#### Scenario: Successful login
- **WHEN** a registered user submits the correct email and password
- **THEN** the system returns a session token valid for the configured lifetime

#### Scenario: Invalid credentials
- **WHEN** a user submits a wrong password or an unknown email
- **THEN** the system rejects the request with an authentication error that does not reveal which field was wrong

#### Scenario: Expired or invalid token rejected
- **WHEN** a request presents an expired or malformed session token
- **THEN** the system rejects the request as unauthenticated

#### Scenario: Logout revokes session
- **WHEN** an authenticated user logs out
- **THEN** the system invalidates the presented session token so it can no longer be used

### Requirement: Password storage

The system SHALL store user passwords only as salted, one-way password hashes and MUST never persist or log plaintext passwords.

#### Scenario: Password never stored in plaintext
- **WHEN** a user registers or changes their password
- **THEN** the stored credential is a salted hash and the plaintext password appears in no persistent store or log

### Requirement: Profile management

The system SHALL let an authenticated user view and update their profile display name, and SHALL let a user change their password after confirming the current password.

#### Scenario: Update display name
- **WHEN** an authenticated user submits a new display name
- **THEN** the system persists it and returns the updated profile

#### Scenario: Change password with correct current password
- **WHEN** an authenticated user submits a new password together with the correct current password
- **THEN** the system stores the new password hash and the user can authenticate with the new password

#### Scenario: Change password with wrong current password
- **WHEN** an authenticated user submits a new password with an incorrect current password
- **THEN** the system rejects the request and leaves the existing password unchanged

### Requirement: Authenticated access to study data

The system SHALL scope all user-owned resources (courses, lessons progress, plans, compilations, and shares) to the authenticated owner and MUST deny access to resources owned by another user.

#### Scenario: Owner can access own resource
- **WHEN** an authenticated user requests a course, plan, compilation, or share they own
- **THEN** the system returns the resource

#### Scenario: Access to another user's private resource denied
- **WHEN** an authenticated user requests a private resource owned by a different user
- **THEN** the system denies access without revealing the resource's contents
