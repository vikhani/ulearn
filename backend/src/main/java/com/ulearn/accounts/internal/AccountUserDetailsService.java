package com.ulearn.accounts.internal;

import com.ulearn.accounts.persistence.CredentialEntity;
import com.ulearn.accounts.persistence.CredentialRepository;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Authenticates against the stored BCrypt credential. The staged multi-user change replaces this with
 * registration/login (ADR-0008).
 */
@Component
@RequiredArgsConstructor
class AccountUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        CredentialEntity credential = credentialRepository
                .findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.getUsername())
                .password(credential.getPasswordHash())
                .roles("USER")
                .build();
    }
}
