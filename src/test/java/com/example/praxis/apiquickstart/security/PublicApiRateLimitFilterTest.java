package com.example.praxis.apiquickstart.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicApiRateLimitFilterTest {

    @Test
    void shouldApplyConfigRuleToAiPatchEndpoint() throws Exception {
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.allow(eq("config:127.0.0.1"), eq(120), eq(60_000L))).thenReturn(true);
        PublicApiRateLimitFilter filter = new PublicApiRateLimitFilter(
                rateLimiterService,
                untrustedProxyPolicy(),
                true,
                10,
                60_000L,
                120,
                60_000L,
                60,
                60_000L,
                5,
                60_000L,
                120,
                60_000L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/praxis/config/ai/patch");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiterService).allow("config:127.0.0.1", 120, 60_000L);
    }

    @Test
    void shouldApplyPublicQueryRuleToSchemasFilteredEndpoint() throws Exception {
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.allow(eq("public-query:127.0.0.1"), eq(60), eq(60_000L))).thenReturn(true);
        PublicApiRateLimitFilter filter = new PublicApiRateLimitFilter(
                rateLimiterService,
                untrustedProxyPolicy(),
                true,
                10,
                60_000L,
                120,
                60_000L,
                60,
                60_000L,
                5,
                60_000L,
                120,
                60_000L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/schemas/filtered");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiterService).allow("public-query:127.0.0.1", 60, 60_000L);
    }

    @Test
    void shouldIgnoreForgedForwardedForWhenPeerIsNotTrustedProxy() throws Exception {
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.allow(eq("config:198.51.100.25"), eq(120), eq(60_000L))).thenReturn(true);
        PublicApiRateLimitFilter filter = filter(rateLimiterService, untrustedProxyPolicy());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/praxis/config/ai/patch");
        request.setRemoteAddr("198.51.100.25");
        request.addHeader("X-Forwarded-For", "203.0.113.77");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiterService).allow("config:198.51.100.25", 120, 60_000L);
    }

    @Test
    void shouldUseForwardedForWhenImmediatePeerIsTrustedProxy() throws Exception {
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.allow(eq("config:203.0.113.77"), eq(120), eq(60_000L))).thenReturn(true);
        PublicApiRateLimitFilter filter = filter(rateLimiterService, trustedProxyPolicy());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/praxis/config/ai/patch");
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.77, 10.0.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiterService).allow("config:203.0.113.77", 120, 60_000L);
    }

    private PublicApiRateLimitFilter filter(RateLimiterService rateLimiterService, TrustedProxyPolicy trustedProxyPolicy) {
        return new PublicApiRateLimitFilter(
                rateLimiterService,
                trustedProxyPolicy,
                true,
                10,
                60_000L,
                120,
                60_000L,
                60,
                60_000L,
                5,
                60_000L,
                120,
                60_000L);
    }

    private TrustedProxyPolicy trustedProxyPolicy() {
        return new TrustedProxyPolicy(true, "10.0.0.10");
    }

    private TrustedProxyPolicy untrustedProxyPolicy() {
        return new TrustedProxyPolicy(false, "");
    }
}
