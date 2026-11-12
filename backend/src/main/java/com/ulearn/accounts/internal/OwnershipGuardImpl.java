package com.ulearn.accounts.internal;

import com.ulearn.accounts.api.CurrentUserProvider;
import com.ulearn.accounts.api.OwnershipGuard;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class OwnershipGuardImpl implements OwnershipGuard {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public void requireOwner(UUID ownerId) {
        UUID currentUserId = currentUserProvider.currentUser().id();
        if (!currentUserId.equals(ownerId)) {
            log.warn("Access denied: user {} is not the owner {}", currentUserId, ownerId);
            throw new AccessDeniedException("Resource not found");
        }
    }
}
