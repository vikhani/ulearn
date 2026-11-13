package com.ulearn.accounts.config;

import com.ulearn.common.web.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Interim authentication: HTTP Basic against the single bootstrap user seeded from environment
 * config (ADR-0008). The {@code user-accounts} spec's multi-user registration/login/session flow is
 * the target and replaces this configuration in a follow-up change, so all the auth wiring lives
 * here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
    // CSRF is disabled deliberately: this is a stateless HTTP Basic API with no cookie/session
    // state, so there is no browser session to forge. Revisit when state-changing endpoints and the
    // multi-user session flow land (ADR-0008).
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers("/health", "/health/**", "/info", "/error")
                .permitAll()
                .anyRequest()
                .authenticated())
        .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
        .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint));
    return http.build();
  }
}
