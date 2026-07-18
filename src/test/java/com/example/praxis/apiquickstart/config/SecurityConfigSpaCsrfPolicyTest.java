package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.auth.AuthController;
import com.example.praxis.apiquickstart.auth.GovernanceLabIdentityService;
import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.CsrfCookieFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.QuickstartPrincipalAuthorityCatalog;
import com.example.praxis.apiquickstart.security.SpaCsrfTokenRequestHandler;
import com.example.praxis.apiquickstart.security.TrustedProxyPolicy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        QuickstartPrincipalAuthorityCatalog.class,
        CsrfCookieFilter.class,
        SpaCsrfTokenRequestHandler.class,
        SecurityConfigSpaCsrfPolicyTest.DummyStatsController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=false",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=false",
        "app.security.write-disabled=false",
        "app.rate-limit.enabled=false",
        "app.session.cookie-name=SESSION",
        "app.session.secure=false",
        "app.session.samesite=Lax"
})
class SecurityConfigSpaCsrfPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private GovernanceLabIdentityService governanceLabIdentityService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldAllowAuthenticatedApiPostWhenXsrfCookieAndHeaderMatch() throws Exception {
        given(jwtTokenService.generate(eq("admin"), eq("ADMIN"), anyCollection())).willReturn("good-token");
        given(jwtTokenService.getExpirationSeconds()).willReturn(3600L);
        given(jwtTokenService.validate("good-token"))
                .willReturn(JwtTokenService.JwtValidationResult.valid("admin", "ADMIN", java.util.List.of()));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"changeMe!"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertNotNull(sessionCookie);

        MvcResult sessionResult = mockMvc.perform(get("/auth/session").cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie csrfCookie = sessionResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        mockMvc.perform(post("/api/test/stats/group-by")
                        .cookie(sessionCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter":{"ativo":true},"field":"ativo","metric":{"operation":"COUNT"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void shouldProjectConfiguredGovernanceAuthorityAsServletRole() throws Exception {
        String authority = com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.COMPOSITION_APPROVER;
        given(governanceLabIdentityService.authenticate("reviewer-a", "reviewer-password"))
                .willReturn(java.util.Optional.of(new GovernanceLabIdentityService.AuthenticatedIdentity(
                        "reviewer-a", "GOVERNANCE_APPROVER", java.util.Set.of(authority))));
        given(jwtTokenService.generate(eq("reviewer-a"), eq("GOVERNANCE_APPROVER"), anyCollection()))
                .willReturn("governance-token");
        given(jwtTokenService.getExpirationSeconds()).willReturn(3600L);
        given(jwtTokenService.validate("governance-token"))
                .willReturn(JwtTokenService.JwtValidationResult.valid(
                        "reviewer-a", "GOVERNANCE_APPROVER", java.util.List.of(authority)));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reviewer-a","password":"reviewer-password"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn();

        mockMvc.perform(get("/api/test/rule-governance-role")
                        .cookie(loginResult.getResponse().getCookie("SESSION")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @RestController
    static class DummyStatsController {

        @PostMapping("/api/test/stats/group-by")
        String groupBy(@RequestBody String body) {
            return "ok";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/test/rule-governance-role")
        String governanceRole(HttpServletRequest request) {
            return Boolean.toString(request.isUserInRole("RULE_COMPOSITION_APPROVER"));
        }
    }
}
