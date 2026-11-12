package com.ulearn.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ulearn.bootstrap")
public class BootstrapUserProperties {

    @NotBlank(message = "BOOTSTRAP_USERNAME must be set")
    private String username;

    @NotBlank(message = "BOOTSTRAP_PASSWORD must be set")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
