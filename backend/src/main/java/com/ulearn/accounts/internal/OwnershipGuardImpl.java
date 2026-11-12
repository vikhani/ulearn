package com.ulearn.accounts.internal;

import com.ulearn.accounts.api.CurrentUserProvider;
import com.ulearn.accounts.api.OwnershipGuard;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class OwnershipGuardImpl implements OwnershipGuard {

    private final CurrentUserProvider currentUserProvider;

    OwnershipGuardImpl(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void requireOwner(UUID ownerId) {
        if (!currentUserProvider.currentUser().id().equals(ownerId)) {
            throw new AccessDeniedException("Authenticated user is not the owner of this resource");
        }
    }
}
