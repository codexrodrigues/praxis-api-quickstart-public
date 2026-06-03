package com.example.praxis.apiquickstart.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigOriginRestrictionFilterTest {

    private static final String ALLOWED =
            "https://praxis-ui-4e602.web.app,https://praxis-ui-4e602.firebaseapp.com," +
            "http://localhost:4200,http://localhost:4003,http://127.0.0.1:4003,http://127.0.0.1:4200," +
            "https://praxisui-dev.web.app,https://praxisui.dev,https://praxisui.com,https://praxisui.vu," +
            "http://127.0.0.1:4301,http://localhost:4301";

    @Test
    void shouldAllowWhenOriginHeaderIsAllowed() throws Exception {
        MockHttpServletResponse response = execute(request -> {
            request.addHeader("Origin", "http://localhost:4003");
        });

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowWhenOriginIsNullButRefererResolvesToAllowedOrigin() throws Exception {
        MockHttpServletResponse response = execute(request -> {
            request.addHeader("Origin", "null");
            request.addHeader("Referer", "http://localhost:4003/demo/event-registration-template");
        });

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowWhenForwardedHeadersResolveToAllowedOrigin() throws Exception {
        MockHttpServletResponse response = execute(request -> {
            request.addHeader("X-Forwarded-Proto", "http");
            request.addHeader("X-Forwarded-Host", "localhost:4003");
        });

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowWhenHostAndSchemeResolveToAllowedOrigin() throws Exception {
        MockHttpServletResponse response = execute(request -> {
            request.setScheme("http");
            request.addHeader("Host", "localhost:4003");
        });

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldBlockWhenNoResolvableOriginMatchesWhitelist() throws Exception {
        MockHttpServletResponse response = execute(request -> {
            request.addHeader("Origin", "https://evil.example");
            request.addHeader("Referer", "https://evil.example/form");
            request.addHeader("Host", "evil.example");
            request.setScheme("https");
        });

        assertEquals(403, response.getStatus());
        assertEquals("{\"status\":\"failure\",\"message\":\"Config origin not allowed\"}", response.getContentAsString());
    }

    @Test
    void shouldBypassForNonConfigPaths() throws Exception {
        ConfigOriginRestrictionFilter filter = new ConfigOriginRestrictionFilter(true, ALLOWED);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/human-resources/funcionarios/all");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletResponse execute(RequestCustomizer customizer) throws ServletException, IOException {
        ConfigOriginRestrictionFilter filter = new ConfigOriginRestrictionFilter(true, ALLOWED);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/praxis/config/ui");
        request.setScheme("http");
        request.setServerName("praxis-api-quickstart.onrender.com");
        request.setServerPort(443);
        request.addParameter("componentType", "praxis-filter");
        request.addParameter("componentId", "filter-schema-meta:rk=funcionarios|ct=praxis-filter|id=funcionarios|ik=0");
        if (customizer != null) {
            customizer.customize(request);
        }
        if (request.getHeader("Host") == null) {
            request.addHeader("Host", "praxis-api-quickstart.onrender.com");
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @FunctionalInterface
    private interface RequestCustomizer {
        void customize(MockHttpServletRequest request);
    }
}
