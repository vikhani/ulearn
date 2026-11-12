package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OwnershipGuardImplTest {

    @Test
    void allowsTheOwner() {
        OwnershipGuardImpl guard = new OwnershipGuardImpl(() -> "learner");

        assertThatCode(() -> guard.requireOwner("learner")).doesNotThrowAnyException();
    }

    @Test
    void deniesAnIdentityThatIsNotTheOwner() {
        OwnershipGuardImpl guard = new OwnershipGuardImpl(() -> "learner");

        assertThatThrownBy(() -> guard.requireOwner("someone-else")).isInstanceOf(AccessDeniedException.class);
    }
}
