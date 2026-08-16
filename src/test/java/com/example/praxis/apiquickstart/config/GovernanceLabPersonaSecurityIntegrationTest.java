package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.praxis.apiquickstart.auth.AuthController;
import com.example.praxis.apiquickstart.auth.GovernanceLabIdentityService;
import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.QuickstartPrincipalAuthorityCatalog;
import com.example.praxis.apiquickstart.security.TrustedProxyPolicy;
import jakarta.servlet.http.Cookie;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proves that the opt-in lab identities cross the real login, JWT and Spring Security chain with
 * mutually exclusive governance responsibilities. Route-level matcher tests remain focused in
 * their own suites; this test prevents the configured persona catalog from drifting away from
 * those matchers.
 */
@WebMvcTest(controllers = {
        AuthController.class,
        GovernanceLabPersonaSecurityIntegrationTest.GovernanceProbeController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        JwtTokenService.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        QuickstartPrincipalAuthorityCatalog.class,
        GovernanceLabIdentityService.class,
        GovernanceLabPersonaSecurityIntegrationTest.GovernanceProbeController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=true",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false",
        "app.jwt.secret=governance-persona-test-secret",
        "app.jwt.exp-min=60",
        "app.session.cookie-name=SESSION",
        "app.session.secure=false",
        "app.session.samesite=Lax",
        "spring.security.user.name=admin",
        "spring.security.user.password=admin-test-password",
        "praxis.ai.security.corporate-mode=true",
        "app.auth.governance-lab.enabled=true",
        "app.auth.governance-lab.author.username=policy-author",
        "app.auth.governance-lab.author.password=author-test-password",
        "app.auth.governance-lab.approver-a.username=policy-approver-a",
        "app.auth.governance-lab.approver-a.password=approver-a-test-password",
        "app.auth.governance-lab.approver-b.username=policy-approver-b",
        "app.auth.governance-lab.approver-b.password=approver-b-test-password",
        "app.auth.governance-lab.publisher.username=policy-publisher",
        "app.auth.governance-lab.publisher.password=publisher-test-password",
        "app.auth.governance-lab.operator.username=policy-operator",
        "app.auth.governance-lab.operator.password=operator-test-password",
        "app.auth.governance-lab.auditor.username=policy-auditor",
        "app.auth.governance-lab.auditor.password=auditor-test-password"
})
class GovernanceLabPersonaSecurityIntegrationTest {

    private static final String DEFINITIONS = "/api/praxis/config/domain-rules/definitions";
    private static final String WORKSPACES = "/api/praxis/config/domain-rules/workspaces";
    private static final String REVIEW = WORKSPACES + "/workspace-a/reviews";
    private static final String COMPOSITION_APPROVAL =
            "/api/praxis/policy-studio/rule-sets/reference-rules/candidate/approvals";
    private static final String PUBLICATION = "/api/praxis/config/domain-rules/publications";
    private static final String ACTIVATE =
            "/api/praxis/config/domain-rules/snapshots/snapshot-a/activate";
    private static final String OPERATIONAL_TEST =
            "/api/human-resources/extraordinary-benefit-requests/actions/run-policy-studio-operational-test";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void configuredPersonasRemainSeparatedAcrossLoginAndSessionSwitches() throws Exception {
        mockMvc.perform(get(DEFINITIONS)).andExpect(result -> assertDenied(result.getResponse().getStatus()));

        Cookie author = login("policy-author", "author-test-password");
        expectAllowed(author, DEFINITIONS, WORKSPACES);
        expectForbidden(author, REVIEW, COMPOSITION_APPROVAL, PUBLICATION, ACTIVATE, OPERATIONAL_TEST);

        Cookie approverA = switchTo(author, "approver-a");
        expectAllowed(approverA, DEFINITIONS, REVIEW, COMPOSITION_APPROVAL);
        expectForbidden(approverA, WORKSPACES, PUBLICATION, ACTIVATE, OPERATIONAL_TEST);

        Cookie approverB = switchTo(approverA, "approver-b");
        expectAllowed(approverB, DEFINITIONS, REVIEW, COMPOSITION_APPROVAL);
        expectForbidden(approverB, WORKSPACES, PUBLICATION, ACTIVATE, OPERATIONAL_TEST);

        Cookie publisher = switchTo(approverB, "publisher");
        expectAllowed(publisher, DEFINITIONS, PUBLICATION);
        expectForbidden(publisher, WORKSPACES, REVIEW, COMPOSITION_APPROVAL, ACTIVATE, OPERATIONAL_TEST);

        Cookie operator = switchTo(publisher, "operator");
        expectAllowed(operator, DEFINITIONS, ACTIVATE, OPERATIONAL_TEST);
        expectForbidden(operator, WORKSPACES, REVIEW, COMPOSITION_APPROVAL, PUBLICATION);

        Cookie auditor = switchTo(operator, "auditor");
        expectAllowed(auditor, DEFINITIONS);
        expectForbidden(auditor, WORKSPACES, REVIEW, COMPOSITION_APPROVAL, PUBLICATION, ACTIVATE,
                OPERATIONAL_TEST);
    }

    private Cookie login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return cookie;
    }

    private Cookie switchTo(Cookie currentSession, String identityKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/governance-lab/session/" + identityKey)
                        .cookie(currentSession))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return cookie;
    }

    private void expectAllowed(Cookie session, String... paths) throws Exception {
        for (String path : paths) {
            if (DEFINITIONS.equals(path)) {
                mockMvc.perform(get(path).cookie(session)).andExpect(status().isOk());
            } else {
                mockMvc.perform(post(path).cookie(session)).andExpect(status().isOk());
            }
        }
    }

    private void expectForbidden(Cookie session, String... paths) throws Exception {
        for (String path : paths) {
            mockMvc.perform(post(path).cookie(session)).andExpect(status().isForbidden());
        }
    }

    private static void assertDenied(int status) {
        if (status != 401 && status != 403) {
            throw new AssertionError("Expected 401 or 403, got " + status);
        }
    }

    @RestController
    static class GovernanceProbeController {
        @GetMapping(DEFINITIONS)
        String definitions() {
            return "definitions";
        }

        @PostMapping({
                WORKSPACES,
                REVIEW,
                COMPOSITION_APPROVAL,
                PUBLICATION,
                ACTIVATE,
                OPERATIONAL_TEST
        })
        String command() {
            return "accepted";
        }
    }
}
