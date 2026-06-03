package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void shouldAllowPatchPreflightForRuntimeConfigApis() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) new SecurityConfig().corsConfigurationSource("*");

        CorsConfiguration configuration = source.getCorsConfigurations().get("/**");

        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
    }
}
