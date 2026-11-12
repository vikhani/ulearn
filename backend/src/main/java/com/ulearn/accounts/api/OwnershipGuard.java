package com.ulearn.accounts.api;

/**
 * Enforces that the authenticated user owns a resource before a capability acts on it. Capabilities
 * store the owner as the authenticated username and call this when serving a request.
 */
public interface OwnershipGuard {

    /**
     * @param owner the username recorded as the resource's owner
     * @throws org.springframework.security.access.AccessDeniedException when the authenticated user
     *     is not {@code owner}
     */
    void requireOwner(String owner);
}
