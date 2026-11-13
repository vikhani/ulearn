package com.ulearn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for integration tests. It boots the full application against a Testcontainers
 * PostgreSQL database and configures MockMvc, so integration tests share one cached application
 * context instead of each declaring its own annotations. Subclasses add {@code @Transactional} when
 * they need rollback and {@code @MockitoBean} to replace collaborators. Tests that need different
 * configuration (for example explicit properties or extra {@code @Import}s) may still extend this
 * and add those annotations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {

  @Autowired protected MockMvc mockMvc;
}
