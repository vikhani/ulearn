package com.ulearn.accounts.internal;

import com.ulearn.accounts.persistence.CredentialEntity;
import com.ulearn.accounts.persistence.CredentialRepository;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import com.ulearn.config.BootstrapUserProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the single bootstrap account from environment config on startup, idempotently: the user row
 * is created once, and its credential is kept in sync with the configured password as a BCrypt hash
 * (ADR-0008). The password value itself is never logged or persisted in plaintext.
 */
@Component
@RequiredArgsConstructor
class BootstrapUserInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final BootstrapUserProperties bootstrapUserProperties;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String username = bootstrapUserProperties.getUsername();
    UserEntity user =
        userRepository
            .findByUsername(username)
            .orElseGet(() -> userRepository.save(new UserEntity(username, username)));

    String passwordHash = passwordEncoder.encode(bootstrapUserProperties.getPassword());
    credentialRepository
        .findById(user.getId())
        .ifPresentOrElse(
            credential -> credential.setPasswordHash(passwordHash),
            () -> credentialRepository.save(new CredentialEntity(user.getId(), passwordHash)));
  }
}
