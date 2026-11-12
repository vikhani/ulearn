package com.ulearn.accounts.api;

import java.util.UUID;

/**
 * Enforces that the authenticated user owns a resource before a capability acts on it. Capabilities
 * store the owner's id and call this when serving a request.
 */
public interface OwnershipGuard {

    /**
     * @param ownerId the id recorded as the resource's owner
     * @throws org.springframework.security.access.AccessDeniedException when the authenticated user is
     *     not the owner
     */
    void requireOwner(UUID ownerId);
}
