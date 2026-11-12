package com.ulearn.accounts.web;

import com.ulearn.accounts.api.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the authenticated identity so clients can confirm their Basic-auth credentials. Under the
 * staged single-user model this is the bootstrap user (ADR-0008).
 */
@RestController
@RequestMapping("/api/account")
class AccountController {

    private final CurrentUserProvider currentUserProvider;

    AccountController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    AccountResponse currentAccount() {
        return new AccountResponse(currentUserProvider.currentUsername());
    }

    record AccountResponse(String username) {}
}
