package com.example.praxis.apiquickstart.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Prevents the pedagogical local identity defaults from reaching a production deployment. */
@Component
@Profile("prod")
final class ProductionDemoAuthenticationGuard {
    private static final String DEFAULT_PASSWORD = "changeMe!";
    private static final String DEFAULT_JWT_SECRET = "dev-secret-change-me";

    private final String adminPassword;
    private final String jwtSecret;

    ProductionDemoAuthenticationGuard(
            @Value("${spring.security.user.password:}") String adminPassword,
            @Value("${app.jwt.secret:}") String jwtSecret) {
        this.adminPassword = adminPassword;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void validate() {
        if (adminPassword == null || adminPassword.length() < 12 || DEFAULT_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "Production requires a non-default demo admin password with at least 12 characters");
        }
        if (jwtSecret == null || jwtSecret.length() < 32 || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Production requires a non-default JWT signing secret with at least 32 characters");
        }
    }
}
