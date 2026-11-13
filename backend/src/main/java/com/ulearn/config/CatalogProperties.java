package com.ulearn.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ulearn.catalog")
public class CatalogProperties {

  /**
   * How old a Playlist snapshot may be before it is considered stale and refreshed on add and by
   * the refresh job (ADR-0009). Defaults to one week to match the weekly cadence.
   */
  @NotNull private Duration freshnessThreshold = Duration.ofDays(7);

  public Duration getFreshnessThreshold() {
    return freshnessThreshold;
  }

  public void setFreshnessThreshold(Duration freshnessThreshold) {
    this.freshnessThreshold = freshnessThreshold;
  }
}
