package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulearn.accounts.api.AuthenticatedUser;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserProviderImpl provider = new CurrentUserProviderImpl(userRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        username, "credentials", AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    @Test
    void returnsTheRegisteredUserForTheAuthenticatedPrincipal() {
        UUID id = UUID.randomUUID();
        UserEntity entity = mock(UserEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getUsername()).thenReturn("learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(entity));
        authenticateAs("learner");

        assertThat(provider.currentUser()).isEqualTo(new AuthenticatedUser(id, "learner"));
    }

    @Test
    void rejectsWhenThereIsNoAuthentication() {
        assertThatThrownBy(provider::currentUser).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(provider::currentUser).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnAuthenticatedPrincipalWithNoRegisteredUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        authenticateAs("ghost");

        assertThatThrownBy(provider::currentUser).isInstanceOf(AccessDeniedException.class);
    }
}
