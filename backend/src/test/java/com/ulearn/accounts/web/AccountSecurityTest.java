package com.ulearn.accounts.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ulearn.TestcontainersConfiguration;
import com.ulearn.accounts.api.OwnershipGuard;
import com.ulearn.accounts.persistence.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, AccountSecurityTest.OwnedResourceTestConfig.class})
class AccountSecurityTest {

  @Autowired MockMvc mockMvc;

  @Autowired UserRepository userRepository;

  @Test
  void correctBasicCredentialsAreAuthenticated() throws Exception {
    mockMvc
        .perform(get("/api/account").with(httpBasic("test-user", "test-password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("test-user"))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void wrongBasicCredentialsAreRejected() throws Exception {
    mockMvc
        .perform(get("/api/account").with(httpBasic("test-user", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void missingCredentialsAreRejected() throws Exception {
    mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
  }

  @Test
  void healthEndpointStaysPublic() throws Exception {
    mockMvc.perform(get("/health")).andExpect(status().isOk());
  }

  @Test
  void ownerCanAccessOwnResource() throws Exception {
    mockMvc
        .perform(
            get("/test/owned-resource/" + authenticatedUserId())
                .with(httpBasic("test-user", "test-password")))
        .andExpect(status().isOk())
        .andExpect(content().string("contents"));
  }

  @Test
  void accessToAnotherIdentitysResourceIsDenied() throws Exception {
    mockMvc
        .perform(
            get("/test/owned-resource/" + UUID.randomUUID())
                .with(httpBasic("test-user", "test-password")))
        .andExpect(status().isForbidden());
  }

  private UUID authenticatedUserId() {
    return userRepository.findByUsername("test-user").orElseThrow().getId();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class OwnedResourceTestConfig {

    @RestController
    static class OwnedResourceController {

      private final OwnershipGuard ownershipGuard;

      OwnedResourceController(OwnershipGuard ownershipGuard) {
        this.ownershipGuard = ownershipGuard;
      }

      @GetMapping("/test/owned-resource/{ownerId}")
      String read(@PathVariable UUID ownerId) {
        ownershipGuard.requireOwner(ownerId);
        return "contents";
      }
    }
  }
}
