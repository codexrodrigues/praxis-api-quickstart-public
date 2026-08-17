package com.example.praxis.apiquickstart.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@WebMvcTest(controllers = SecurityConfigDomainRuleReadPolicyTest.DomainRuleProbeController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        SecurityConfigDomainRuleReadPolicyTest.DomainRuleProbeController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=true",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false"
})
class SecurityConfigDomainRuleReadPolicyTest {

    private static final String DEFINITIONS = "/api/praxis/config/domain-rules/definitions";
    private static final String DEFINITION_CATALOG = DEFINITIONS + "/catalog";
    private static final String TIMELINE = DEFINITIONS + "/definition-a/timeline";
    private static final String SIMULATIONS = "/api/praxis/config/domain-rules/simulations";
    private static final String WORKSPACES = "/api/praxis/config/domain-rules/workspaces";
    private static final String WORKSPACE_DRAFT = WORKSPACES + "/workspace-a/draft";
    private static final String WORKSPACE_REVIEW = WORKSPACES + "/workspace-a/reviews";
    private static final String SANDBOX_RUNS = "/api/praxis/policy-studio/sandbox/runs";
    private static final String OPERATIONAL_TEST_RUNS =
            "/api/human-resources/extraordinary-benefit-requests/actions/run-policy-studio-operational-test";
    private static final String RULESET_CANDIDATE =
            "/api/praxis/policy-studio/rule-sets/benefit-rules/candidate";
    private static final String RULESET_CANDIDATE_APPROVAL = RULESET_CANDIDATE + "/approvals";
    private static final String RULESET_CANDIDATE_PUBLISH = RULESET_CANDIDATE + "/publish";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void readOpenDoesNotExposeGovernedDefinitionsOrTimeline() throws Exception {
        mockMvc.perform(get(DEFINITIONS)).andExpect(result -> assertDenied(result.getResponse().getStatus()));
        mockMvc.perform(get(DEFINITION_CATALOG)).andExpect(result -> assertDenied(result.getResponse().getStatus()));
        mockMvc.perform(get(TIMELINE)).andExpect(result -> assertDenied(result.getResponse().getStatus()));
        mockMvc.perform(get(WORKSPACES)).andExpect(result -> assertDenied(result.getResponse().getStatus()));
    }

    @Test
    void definitionReaderCanReadButCannotRunAuthoringSimulation() throws Exception {
        when(jwtTokenService.validate("reader")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "auditor", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_READER)));

        mockMvc.perform(get(DEFINITIONS).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(DEFINITION_CATALOG).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(TIMELINE).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(get(WORKSPACES).header("Authorization", "Bearer reader"))
                .andExpect(status().isOk());
        mockMvc.perform(post(WORKSPACES).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(WORKSPACE_REVIEW).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(SANDBOX_RUNS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(OPERATIONAL_TEST_RUNS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(SIMULATIONS).header("Authorization", "Bearer reader"))
                .andExpect(status().isForbidden());
    }

    @Test
    void definitionAuthorCanRunStructuralSimulation() throws Exception {
        when(jwtTokenService.validate("author")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "policy-author", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_AUTHOR)));

        mockMvc.perform(post(SIMULATIONS).header("Authorization", "Bearer author"))
                .andExpect(status().isOk());
        mockMvc.perform(post(WORKSPACES).header("Authorization", "Bearer author"))
                .andExpect(status().isOk());
        mockMvc.perform(post(WORKSPACE_REVIEW).header("Authorization", "Bearer author"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(WORKSPACE_DRAFT).header("Authorization", "Bearer author"))
                .andExpect(status().isOk());
        mockMvc.perform(post(SANDBOX_RUNS).header("Authorization", "Bearer author"))
                .andExpect(status().isOk());
        mockMvc.perform(post(OPERATIONAL_TEST_RUNS).header("Authorization", "Bearer author"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationalTestOperatorCanRunOnlyTheDedicatedHostProofAction() throws Exception {
        when(jwtTokenService.validate("operational-test-operator")).thenReturn(
                JwtTokenService.JwtValidationResult.valid(
                        "policy-proof-operator", "HUMAN",
                        List.of(RuleGovernanceAuthorities.OPERATIONAL_TEST_OPERATOR)));

        mockMvc.perform(post(OPERATIONAL_TEST_RUNS)
                        .header("Authorization", "Bearer operational-test-operator"))
                .andExpect(status().isOk());
        mockMvc.perform(post(SANDBOX_RUNS)
                        .header("Authorization", "Bearer operational-test-operator"))
                .andExpect(status().isForbidden());
    }

    @Test
    void definitionApproverCanReviewButCannotCreateWorkspace() throws Exception {
        when(jwtTokenService.validate("approver")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "policy-reviewer", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_APPROVER)));

        mockMvc.perform(post(WORKSPACE_REVIEW).header("Authorization", "Bearer approver"))
                .andExpect(status().isOk());
        mockMvc.perform(post(WORKSPACES).header("Authorization", "Bearer approver"))
                .andExpect(status().isForbidden());
    }

    @Test
    void snapshotPublisherPreparesAndPublishesButCannotApproveHostComposition() throws Exception {
        when(jwtTokenService.validate("publisher")).thenReturn(JwtTokenService.JwtValidationResult.valid(
                "release-manager", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER)));

        mockMvc.perform(post(RULESET_CANDIDATE).header("Authorization", "Bearer publisher"))
                .andExpect(status().isOk());
        mockMvc.perform(post(RULESET_CANDIDATE_PUBLISH).header("Authorization", "Bearer publisher"))
                .andExpect(status().isOk());
        mockMvc.perform(post(RULESET_CANDIDATE_APPROVAL).header("Authorization", "Bearer publisher"))
                .andExpect(status().isForbidden());
    }

    @Test
    void compositionApproverCanReviewAndApproveButCannotPublishHostComposition() throws Exception {
        when(jwtTokenService.validate("composition-approver")).thenReturn(
                JwtTokenService.JwtValidationResult.valid(
                        "independent-reviewer", "HUMAN",
                        List.of(RuleGovernanceAuthorities.COMPOSITION_APPROVER)));

        mockMvc.perform(post(RULESET_CANDIDATE_APPROVAL)
                        .header("Authorization", "Bearer composition-approver"))
                .andExpect(status().isOk());
        mockMvc.perform(post(RULESET_CANDIDATE)
                        .header("Authorization", "Bearer composition-approver"))
                .andExpect(status().isOk());
        mockMvc.perform(post(RULESET_CANDIDATE_PUBLISH)
                        .header("Authorization", "Bearer composition-approver"))
                .andExpect(status().isForbidden());
    }

    private static void assertDenied(int status) {
        if (status != 401 && status != 403) {
            throw new AssertionError("Expected 401 or 403, got " + status);
        }
    }

    @RestController
    static class DomainRuleProbeController {
        @GetMapping({DEFINITIONS, DEFINITION_CATALOG, TIMELINE, WORKSPACES})
        String read() {
            return "governed";
        }

        @PostMapping({
                SIMULATIONS,
                WORKSPACES,
                SANDBOX_RUNS,
                OPERATIONAL_TEST_RUNS,
                WORKSPACE_REVIEW,
                RULESET_CANDIDATE,
                RULESET_CANDIDATE_APPROVAL,
                RULESET_CANDIDATE_PUBLISH
        })
        String simulate() {
            return "structural-readiness";
        }

        @org.springframework.web.bind.annotation.PutMapping(WORKSPACE_DRAFT)
        String updateDraft() {
            return "draft-updated";
        }
    }
}
