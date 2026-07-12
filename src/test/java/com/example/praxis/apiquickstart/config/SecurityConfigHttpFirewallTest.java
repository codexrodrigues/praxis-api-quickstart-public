package com.example.praxis.apiquickstart.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigHttpFirewallTest {

    private final HttpFirewall firewall = new SecurityConfig().allowUrlEncodedSlashFirewall();

    @Test
    void shouldAllowEncodedSlashForCanonicalConfigPathIdentifiers() {
        MockHttpServletRequest request = request(
                "/api/praxis/config/ui/praxis-table/screen%2Fpayroll%2Fevents");

        assertDoesNotThrow(() -> firewall.getFirewalledRequest(request));
    }

    @Test
    void shouldKeepEncodedDoubleSlashRejected() {
        MockHttpServletRequest request = request(
                "/api/praxis/config/ui/praxis-table/screen%2F%2Fpayroll");

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(request));
    }

    @Test
    void shouldKeepSemicolonPathParametersRejected() {
        MockHttpServletRequest request = request(
                "/api/praxis/config/ui/praxis-table/screen;tenant=desenv");

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(request));
    }

    @Test
    void shouldKeepEncodedPercentRejected() {
        MockHttpServletRequest request = request(
                "/api/praxis/config/ui/praxis-table/screen%252Fpayroll");

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(request));
    }

    @Test
    void shouldKeepNonConfigRoutesOnStrictEncodingPolicy() {
        MockHttpServletRequest request = request(
                "/api/human-resources/funcionarios%2Fshadow");

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(request));
    }

    private MockHttpServletRequest request(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(requestUri);
        request.setRequestURI(requestUri);
        request.setServerName("localhost");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
