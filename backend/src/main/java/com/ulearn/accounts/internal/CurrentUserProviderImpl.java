package com.ulearn.accounts.internal;

import com.ulearn.accounts.api.AuthenticatedUser;
import com.ulearn.accounts.api.CurrentUserProvider;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class CurrentUserProviderImpl implements CurrentUserProvider {

    private final UserRepository userRepository;

    CurrentUserProviderImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AuthenticatedUser currentUser() {
        String username = authenticatedUsername();
        UserEntity user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user is not registered"));
        return new AuthenticatedUser(user.getId(), user.getUsername());
    }

    private String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("No authenticated user");
        }
        return authentication.getName();
    }
}
