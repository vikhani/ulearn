package com.ulearn.accounts.api;

/**
 * Resolves the authenticated user that every capability scopes its resources to. Ownership is
 * derived from the authenticated principal, never from client-supplied identity (ADR-0008).
 */
public interface CurrentUserProvider {

  /**
   * Returns the user authenticated for the current request.
   *
   * @return the currently authenticated user
   * @throws org.springframework.security.access.AccessDeniedException when there is no
   *     authenticated user in the current context
   */
  AuthenticatedUser currentUser();
}
