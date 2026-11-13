package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ulearn.accounts.api.AuthenticatedUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OwnershipGuardImplTest {

  private static final UUID OWNER = UUID.randomUUID();

  @Test
  void allowsTheOwner() {
    OwnershipGuardImpl guard =
        new OwnershipGuardImpl(() -> new AuthenticatedUser(OWNER, "learner"));

    assertThatCode(() -> guard.requireOwner(OWNER)).doesNotThrowAnyException();
  }

  @Test
  void deniesAnIdentityThatIsNotTheOwner() {
    OwnershipGuardImpl guard =
        new OwnershipGuardImpl(() -> new AuthenticatedUser(OWNER, "learner"));

    assertThatThrownBy(() -> guard.requireOwner(UUID.randomUUID()))
        .isInstanceOf(AccessDeniedException.class);
  }
}
