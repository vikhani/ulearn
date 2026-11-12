package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderImplTest {

    private final CurrentUserProviderImpl provider = new CurrentUserProviderImpl();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheAuthenticatedUsername() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "learner", "credentials", AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThat(provider.currentUsername()).isEqualTo("learner");
    }

    @Test
    void rejectsWhenThereIsNoAuthentication() {
        assertThatThrownBy(provider::currentUsername).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(provider::currentUsername).isInstanceOf(AccessDeniedException.class);
    }
}
