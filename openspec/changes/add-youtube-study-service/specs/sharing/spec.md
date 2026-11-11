# Spec Delta

## Purpose

Lets a user publish a read-only view of a course or compilation through an unguessable, revocable link so others can follow the same study path.

## ADDED Requirements

### Requirement: Create a share link

The system SHALL let the owner create a share link for a course or compilation they own, and the link SHALL be backed by an unguessable token.

#### Scenario: Share link created
- **WHEN** the owner creates a share link for a course or compilation they own
- **THEN** the system returns a URL containing a random token that grants read-only access

#### Scenario: Sharing a resource the user does not own
- **WHEN** a user attempts to share a course or compilation owned by another user
- **THEN** the system denies the request and creates no share link

#### Scenario: Token is not guessable
- **WHEN** a share token is generated
- **THEN** it is drawn from a cryptographically strong random source with sufficient entropy that it cannot be guessed from other tokens

### Requirement: Public read-only view

The system SHALL serve a read-only view of a shared course or compilation to anyone holding a valid share token, without requiring authentication, and SHALL expose only its contents — never any user's progress — and SHALL NOT expose private owner data through it.

#### Scenario: Anonymous visitor views shared resource
- **WHEN** an anonymous visitor opens a valid share link
- **THEN** the system returns the shared course or compilation contents in read-only form

#### Scenario: Shared view excludes progress
- **WHEN** a shared course or compilation is viewed
- **THEN** the response contains no lesson or course progress for any user

#### Scenario: Shared view grants no import
- **WHEN** a visitor views a shared course or compilation
- **THEN** the system provides no ability to copy or import it into an account

#### Scenario: No private data exposed
- **WHEN** a shared resource is viewed
- **THEN** the response excludes the owner's private data and any resources not part of the shared item

#### Scenario: Invalid or revoked token
- **WHEN** a visitor opens a share link whose token is unknown or has been revoked
- **THEN** the system responds as not found and reveals no resource contents

### Requirement: Revoke a share link

The system SHALL let the owner revoke any share link they created, after which the link SHALL stop granting access.

#### Scenario: Revoked link stops working
- **WHEN** the owner revokes a share link
- **THEN** subsequent requests using that token are denied and reveal no resource contents

#### Scenario: Only owner can revoke
- **WHEN** a user attempts to revoke a share link for a resource they do not own
- **THEN** the system denies the request and the link remains valid

### Requirement: Visibility controls

The system SHALL let the owner control whether a course or compilation is private or shared, and SHALL default new resources to private.

#### Scenario: Default is private
- **WHEN** a user creates a course or compilation without specifying visibility
- **THEN** the resource is private and accessible only to its owner

#### Scenario: Making a resource private revokes public access
- **WHEN** the owner sets a previously shared resource back to private
- **THEN** existing share links stop granting access

### Requirement: Shares reflect current contents

The system SHALL serve the current contents of a shared resource, so that updates made by the owner are reflected to visitors holding a valid link.

#### Scenario: Updated content shown to visitors
- **WHEN** the owner updates a shared course or compilation
- **THEN** visitors using a valid share link see the updated contents on their next view
