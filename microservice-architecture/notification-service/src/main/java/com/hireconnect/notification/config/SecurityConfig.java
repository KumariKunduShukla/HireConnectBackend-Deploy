package com.hireconnect.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * FIX 1 — SECURITY CONFIGURATION (NEW FILE — was completely missing)
 *
 * Problem:
 *   spring-boot-starter-security is in pom.xml.
 *   When Spring Security is on the classpath with NO configuration class,
 *   it locks down EVERY endpoint by default — every request returns HTTP 401
 *   "Full authentication is required to access this resource."
 *   This means NO notification endpoint was reachable at all.
 *
 * Fix:
 *   This configuration permits all requests to /api/notifications/** so the
 *   notification service can be called freely (auth is handled at the API Gateway level).
 *   CSRF is disabled because this is a stateless REST API (no browser sessions).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST APIs don't use browser sessions, CSRF protection is irrelevant
            .csrf(AbstractHttpConfigurer::disable)

            // Permit all requests — the API Gateway handles auth before requests reach here
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}