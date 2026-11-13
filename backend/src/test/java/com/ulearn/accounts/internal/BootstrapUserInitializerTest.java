package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ulearn.BaseIntegrationTest;
import com.ulearn.accounts.persistence.CredentialEntity;
import com.ulearn.accounts.persistence.CredentialRepository;
import com.ulearn.accounts.persistence.UserEntity;
import com.ulearn.accounts.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapUserInitializerTest extends BaseIntegrationTest {

  @Autowired BootstrapUserInitializer initializer;

  @Autowired UserRepository userRepository;

  @Autowired CredentialRepository credentialRepository;

  @Autowired PasswordEncoder passwordEncoder;

  @Test
  void seedsExactlyOneBootstrapUserAndIsIdempotent() {
    assertThat(userRepository.findByUsername("test-user")).isPresent();

    long usersAfterStartup = userRepository.count();
    initializer.run(null);
    initializer.run(null);

    assertThat(userRepository.count()).isEqualTo(usersAfterStartup);
    assertThat(userRepository.findByUsername("test-user")).isPresent();
    assertThat(credentialRepository.findAll()).hasSize(1);
  }

  @Test
  void storesAndRefreshesTheBcryptHashOfTheConfiguredPassword() {
    UserEntity user = userRepository.findByUsername("test-user").orElseThrow();
    CredentialEntity seeded = credentialRepository.findById(user.getId()).orElseThrow();
    assertThat(seeded.getPasswordHash()).isNotEqualTo("test-password");
    assertThat(passwordEncoder.matches("test-password", seeded.getPasswordHash())).isTrue();

    initializer.run(null);

    CredentialEntity refreshed = credentialRepository.findById(user.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("test-password", refreshed.getPasswordHash())).isTrue();
    assertThat(refreshed.getPasswordHash())
        .as("re-seeding re-hashes the configured password instead of leaving the stored value")
        .isNotEqualTo(seeded.getPasswordHash());
  }
}
