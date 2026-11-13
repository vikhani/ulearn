package com.ulearn.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ulearn.progress")
public class ProgressProperties {

  /**
   * Fraction of a Lesson's duration that marks it complete automatically (ADR-0001). Defaults to
   * 95% so a Lesson near its end counts as complete.
   */
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private double completionThreshold = 0.95;

  public double getCompletionThreshold() {
    return completionThreshold;
  }

  public void setCompletionThreshold(double completionThreshold) {
    this.completionThreshold = completionThreshold;
  }
}
