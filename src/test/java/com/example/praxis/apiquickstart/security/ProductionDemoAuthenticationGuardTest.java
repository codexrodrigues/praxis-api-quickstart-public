package com.example.praxis.apiquickstart.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionDemoAuthenticationGuardTest {
    @Test
    void rejectsDocumentedDemoDefaults() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionDemoAuthenticationGuard("changeMe!", "dev-secret-change-me").validate());
    }

    @Test
    void rejectsShortProductionSecrets() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionDemoAuthenticationGuard("short", "also-short").validate());
    }

    @Test
    void acceptsExplicitBoundedProductionConfiguration() {
        assertDoesNotThrow(() -> new ProductionDemoAuthenticationGuard(
                "a-long-random-admin-password", "a-32-byte-or-longer-random-jwt-signing-secret").validate());
    }
}
