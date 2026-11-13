package com.ulearn.accounts.web;

import com.ulearn.accounts.api.AuthenticatedUser;
import com.ulearn.accounts.api.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the authenticated identity so clients can confirm their Basic-auth credentials. Under the
 * staged single-user model this is the bootstrap user (ADR-0008).
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
class AccountController {

  private final CurrentUserProvider currentUserProvider;

  @GetMapping
  AccountResponse currentAccount() {
    AuthenticatedUser user = currentUserProvider.currentUser();
    return new AccountResponse(user.id(), user.username());
  }

  record AccountResponse(UUID id, String username) {}
}
