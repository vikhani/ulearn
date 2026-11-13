package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulearn.accounts.persistence.CredentialEntity;
import com.ulearn.accounts.persistence.CredentialRepository;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AccountUserDetailsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CredentialRepository credentialRepository = mock(CredentialRepository.class);
    private final AccountUserDetailsService service =
            new AccountUserDetailsService(userRepository, credentialRepository);

    @Test
    void loadsTheStoredHashForAKnownUser() {
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getUsername()).thenReturn("learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        CredentialEntity credential = mock(CredentialEntity.class);
        when(credential.getPasswordHash()).thenReturn("$2a$stored-hash");
        when(credentialRepository.findById(USER_ID)).thenReturn(Optional.of(credential));

        UserDetails details = service.loadUserByUsername("learner");

        assertThat(details.getUsername()).isEqualTo("learner");
        assertThat(details.getPassword()).isEqualTo("$2a$stored-hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void rejectsAnUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsAUserWithoutACredential() {
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(credentialRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("learner"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
