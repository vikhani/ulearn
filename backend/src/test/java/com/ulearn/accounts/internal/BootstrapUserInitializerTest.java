package com.ulearn.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ulearn.TestcontainersConfiguration;
import com.ulearn.accounts.persistence.CredentialRepository;
import com.ulearn.accounts.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BootstrapUserInitializerTest {

    @Autowired
    BootstrapUserInitializer initializer;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CredentialRepository credentialRepository;

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
}
