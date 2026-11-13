package com.ulearn.accounts.api;

import java.util.UUID;

/**
 * The identity every capability scopes its resources to. {@code id} is the stable owner key stored
 * on user-owned resources; {@code username} is the human-facing login name (ADR-0008).
 */
public record AuthenticatedUser(UUID id, String username) {}
