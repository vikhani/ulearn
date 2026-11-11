package com.ulearn.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class ExternalConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void readsYouTubeAndBootstrapCredentialsFromConfig() {
        runner.withPropertyValues(
                        "ulearn.youtube.api-key=yt-key",
                        "ulearn.bootstrap.username=learner",
                        "ulearn.bootstrap.password=s3cret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(YouTubeProperties.class).getApiKey()).isEqualTo("yt-key");
                    BootstrapUserProperties bootstrap = context.getBean(BootstrapUserProperties.class);
                    assertThat(bootstrap.getUsername()).isEqualTo("learner");
                    assertThat(bootstrap.getPassword()).isEqualTo("s3cret");
                });
    }

    @Test
    void failsFastWhenYouTubeApiKeyIsAbsent() {
        runner.withPropertyValues(
                        "ulearn.bootstrap.username=learner",
                        "ulearn.bootstrap.password=s3cret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("YOUTUBE_API_KEY must be set");
                });
    }

    @Test
    void failsFastWhenBootstrapCredentialsAreAbsent() {
        runner.withPropertyValues("ulearn.youtube.api-key=yt-key")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("BOOTSTRAP_USERNAME must be set");
                });
    }

    @EnableConfigurationProperties({YouTubeProperties.class, BootstrapUserProperties.class})
    static class PropertiesConfig {
    }
}
