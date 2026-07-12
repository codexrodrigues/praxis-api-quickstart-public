package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void shouldKeepWildcardCorsWithoutCredentialsAndExposePraxisContractHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) new SecurityConfig().corsConfigurationSource("*");

        CorsConfiguration configuration = source.getCorsConfigurations().get("/**");

        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
        assertFalse(configuration.getAllowCredentials());
        assertTrue(configuration.getAllowedOriginPatterns().contains("*"));
        assertEquals(SecurityConfig.PRAXIS_EXPOSED_CORS_HEADERS, configuration.getExposedHeaders());
    }

    @Test
    void shouldAllowCredentialedOfficialLocalOriginsToReadPraxisContractHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) new SecurityConfig().corsConfigurationSource(
                        "http://localhost:4003,http://127.0.0.1:4003");

        CorsConfiguration configuration = source.getCorsConfigurations().get("/**");

        assertTrue(configuration.getAllowCredentials());
        assertEquals(
                java.util.List.of("http://localhost:4003", "http://127.0.0.1:4003"),
                configuration.getAllowedOrigins());
        assertEquals(SecurityConfig.PRAXIS_EXPOSED_CORS_HEADERS, configuration.getExposedHeaders());
        assertTrue(configuration.getExposedHeaders().contains("ETag"));
        assertTrue(configuration.getExposedHeaders().contains("X-Schema-Hash"));
        assertTrue(configuration.getExposedHeaders().contains("X-Data-Version"));
    }

    @Test
    void shouldEmitBrowserReadableExposeHeadersForAllowedCredentialedOrigin() throws Exception {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) new SecurityConfig().corsConfigurationSource("http://localhost:4003");
        MockHttpServletRequest request = requestFrom("http://localhost:4003");
        CorsConfiguration configuration = source.getCorsConfiguration(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(new DefaultCorsProcessor().processRequest(configuration, request, response));

        assertEquals(
                "ETag, X-Schema-Hash, X-Data-Version",
                response.getHeader("Access-Control-Expose-Headers"));
    }

    private static MockHttpServletRequest requestFrom(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/praxis/config/user-config");
        request.addHeader("Origin", origin);
        return request;
    }
}
