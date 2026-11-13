package com.ulearn.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ulearn.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "ulearn.bootstrap.password=unique-bootstrap-secret")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class BootstrapSecretLoggingTest {

  @Test
  void bootstrapPasswordNeverAppearsInStartupLogs(CapturedOutput output) {
    assertThat(output).contains(":: Spring Boot ::");
    assertThat(output).doesNotContain("unique-bootstrap-secret");
  }
}
