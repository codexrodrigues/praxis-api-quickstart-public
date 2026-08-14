package com.example.praxis.apiquickstart.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import com.example.praxis.apiquickstart.security.TrustedProxyPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigRuleSnapshotReadPolicyTest.SnapshotHeadController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        SecurityConfigRuleSnapshotReadPolicyTest.SnapshotHeadController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=true",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false"
})
class SecurityConfigRuleSnapshotReadPolicyTest {

    private static final String HEAD = "/api/praxis/config/domain-rules/snapshots/head";
    private static final String DEFINITIONS = "/api/praxis/config/domain-rules/definitions";
    private static final String TIMELINE = "/api/praxis/config/domain-rules/definitions/00000000-0000-0000-0000-000000000001/timeline";
    private static final String MATERIALIZATIONS = "/api/praxis/config/domain-rules/materializations";
    private static final String ACTIVATE = "/api/praxis/config/domain-rules/snapshots/snapshot-v2/activate";
    private static final String ROLLBACK = "/api/praxis/config/domain-rules/snapshots/snapshot-v1/rollback";
    private static final String HOST_STATUS = "/api/praxis/config/domain-rules/snapshots/host-status";
    private static final String ROLLOUTS = "/api/praxis/config/domain-rules/snapshots/rollouts";
    private static final String ROLLOUT_PROBE = ROLLOUTS + "/rollout-1/probes";
    private static final String ROLLOUT_CANCEL = ROLLOUTS + "/rollout-1/cancel";
    private static final String ROLLOUT_READINESS = ROLLOUTS + "/rollout-1/readiness";
    private static final String ROLLOUT_PENDING = ROLLOUTS + "/pending";
    private static final String ROLLOUT_POLICIES =
            "/api/praxis/config/domain-rules/snapshots/rollout-policies";
    private static final String ROLLOUT_POLICY_APPROVE = ROLLOUT_POLICIES + "/policy-1/approve";
    private static final String ROLLOUT_POLICY_ACTIVATE = ROLLOUT_POLICIES + "/policy-1/activate";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void missingAndInvalidCredentialsCannotReadTheHeadEvenWhenReadOpenIsEnabled() throws Exception {
        mockMvc.perform(get(HEAD)).andExpect(result -> assertDenied(result.getResponse().getStatus()));

        when(jwtTokenService.validate("invalid"))
                .thenReturn(JwtTokenService.JwtValidationResult.invalid("INVALID_SIGNATURE"));
        mockMvc.perform(get(HEAD).header("Authorization", "Bearer invalid"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus()));
    }

    @Test
    void authenticatedPrincipalWithoutReaderAuthorityIsForbidden() throws Exception {
        when(jwtTokenService.validate("publisher-only"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "publisher", "SERVICE", List.of(
                                RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER)));

        mockMvc.perform(get(HEAD).header("Authorization", "Bearer publisher-only"))
                .andExpect(status().isForbidden());
    }

    @Test
    void snapshotReaderCanReachSnapshotButNotDefinitionCatalog() throws Exception {
        when(jwtTokenService.validate("reader"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "ergon-host", "SERVICE", List.of(
                                RuleGovernanceAuthorities.SNAPSHOT_READER)));

        mockMvc.perform(get(HEAD).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(DEFINITIONS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TIMELINE).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(MATERIALIZATIONS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
    }

    @Test
    void domainRuleCatalogReadsAreNotOpenedByTheGeneralConfigPolicy() throws Exception {
        mockMvc.perform(get(DEFINITIONS))
                .andExpect(result -> assertDenied(result.getResponse().getStatus()));
        mockMvc.perform(get(TIMELINE))
                .andExpect(result -> assertDenied(result.getResponse().getStatus()));
        mockMvc.perform(get(MATERIALIZATIONS))
                .andExpect(result -> assertDenied(result.getResponse().getStatus()));
    }

    @Test
    void onlyAnAuthenticatedSnapshotOperatorCanMoveTheHead() throws Exception {
        when(jwtTokenService.validate("reader"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "ergon-host", "SERVICE", List.of(
                                RuleGovernanceAuthorities.SNAPSHOT_READER)));
        mockMvc.perform(post(ACTIVATE).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());

        when(jwtTokenService.validate("operator"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "policy-operator", "HUMAN", List.of(
                                RuleGovernanceAuthorities.SNAPSHOT_OPERATOR)));
        mockMvc.perform(post(ACTIVATE).header("Authorization", "Bearer operator"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ROLLBACK).header("Authorization", "Bearer operator"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAnExecutionObserverServiceCanPublishHostStatus() throws Exception {
        mockMvc.perform(post(HOST_STATUS))
                .andExpect(result -> assertDenied(result.getResponse().getStatus()));

        when(jwtTokenService.validate("reader"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "policy-auditor", "HUMAN", List.of(
                                RuleGovernanceAuthorities.SNAPSHOT_READER)));
        mockMvc.perform(post(HOST_STATUS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());

        when(jwtTokenService.validate("observer"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "service:ergon-host-a", "SERVICE", List.of(
                                RuleGovernanceAuthorities.EXECUTION_OBSERVER)));
        mockMvc.perform(post(HOST_STATUS).header("Authorization", "Bearer observer"))
                .andExpect(status().isAccepted());
    }

    @Test
    void rolloutRoutesKeepOperatorObserverAndReaderAuthoritiesSeparated() throws Exception {
        when(jwtTokenService.validate("reader")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "auditor", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_READER)));
        when(jwtTokenService.validate("operator")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "operator", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_OPERATOR)));
        when(jwtTokenService.validate("observer")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "service:host-a", "SERVICE", List.of(RuleGovernanceAuthorities.EXECUTION_OBSERVER)));

        mockMvc.perform(get(ROLLOUT_READINESS).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(ROLLOUTS).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(ROLLOUTS).header("Authorization", "Bearer observer"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUTS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUTS).header("Authorization", "Bearer operator"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ROLLOUT_CANCEL).header("Authorization", "Bearer operator"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ROLLOUT_PROBE).header("Authorization", "Bearer operator"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUT_PROBE).header("Authorization", "Bearer observer"))
                .andExpect(status().isOk());
        mockMvc.perform(get(ROLLOUT_PENDING).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ROLLOUT_PENDING).header("Authorization", "Bearer observer"))
                .andExpect(status().isOk());
    }

    @Test
    void rolloutPolicyRoutesEnforceMakerCheckerAndOperatorSeparation() throws Exception {
        when(jwtTokenService.validate("author")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "author", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_AUTHOR)));
        when(jwtTokenService.validate("reviewer")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "reviewer", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_APPROVER)));
        when(jwtTokenService.validate("operator")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "operator", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_OPERATOR)));
        when(jwtTokenService.validate("reader")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "auditor", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_READER)));

        mockMvc.perform(post(ROLLOUT_POLICIES).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUT_POLICIES).header("Authorization", "Bearer author"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ROLLOUT_POLICY_APPROVE).header("Authorization", "Bearer author"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUT_POLICY_APPROVE).header("Authorization", "Bearer reviewer"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ROLLOUT_POLICY_ACTIVATE).header("Authorization", "Bearer reviewer"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ROLLOUT_POLICY_ACTIVATE).header("Authorization", "Bearer operator"))
                .andExpect(status().isOk());
        mockMvc.perform(get(ROLLOUT_POLICIES).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
    }

    private static void assertDenied(int status) {
        if (status != 401 && status != 403) {
            throw new AssertionError("Expected 401 or 403, got " + status);
        }
    }

    @RestController
    static class SnapshotHeadController {
        @GetMapping(HEAD)
        String head() {
            return "active";
        }

        @GetMapping({DEFINITIONS, TIMELINE, MATERIALIZATIONS})
        String catalog() {
            return "catalog";
        }

        @PostMapping({ACTIVATE, ROLLBACK})
        String moveHead() {
            return "selected";
        }

        @PostMapping(HOST_STATUS)
        org.springframework.http.ResponseEntity<String> hostStatus() {
            return org.springframework.http.ResponseEntity.accepted().body("observed");
        }

        @PostMapping({ROLLOUTS, ROLLOUT_PROBE, ROLLOUT_CANCEL, ROLLOUT_POLICIES,
                ROLLOUT_POLICY_APPROVE, ROLLOUT_POLICY_ACTIVATE})
        String rolloutWrite() { return "ok"; }

        @GetMapping({ROLLOUTS, ROLLOUT_READINESS, ROLLOUT_POLICIES})
        String rolloutReadiness() { return "ready"; }

        @GetMapping(ROLLOUT_PENDING)
        String pendingRollout() { return "candidate"; }
    }
}
