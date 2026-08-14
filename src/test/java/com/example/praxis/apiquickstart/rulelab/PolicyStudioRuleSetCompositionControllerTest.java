package com.example.praxis.apiquickstart.rulelab;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.config.contract.RuleSetCompositionAction;
import org.praxisplatform.config.contract.RuleSetCompositionCandidate;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateRequest;
import org.praxisplatform.config.contract.RuleSetCompositionSource;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipalResolver;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PolicyStudioRuleSetCompositionControllerTest {
    private static final String PATH =
            "/api/praxis/policy-studio/rule-sets/extraordinary-grant-eligibility/candidate";

    private PolicyStudioRuleSetCompositionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PolicyStudioRuleSetCompositionService.class);
        DomainRuleGovernancePrincipalResolver resolver = mock(DomainRuleGovernancePrincipalResolver.class);
        when(resolver.resolve(any(), any(), any(), any())).thenReturn(
                new DomainRuleGovernancePrincipal("desenv", "publisher", "local"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PolicyStudioRuleSetCompositionController(service, resolver))
                .build();
    }

    @Test
    void returnsOnlySafeCandidateIdentityAndProvenance() throws Exception {
        UUID definitionId = UUID.randomUUID();
        when(service.prepare(
                eq("extraordinary-grant-eligibility"),
                any(RuleSetCompositionCandidateRequest.class),
                any(DomainRuleGovernancePrincipal.class),
                any()))
                .thenReturn(new RuleSetCompositionCandidate(
                        "extraordinary-grant-eligibility", 2, "A".repeat(64), "B".repeat(64),
                        "head-etag", List.of(new RuleSetCompositionSource(
                                definitionId, "grant.amount-parameters", 3, "approved")),
                        List.of(RuleSetCompositionAction.PREPARE, RuleSetCompositionAction.PUBLISH)));

        mockMvc.perform(post(PATH)
                        .principal(publisher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promotedDefinitionId": "%s",
                                  "validFromUtc": "2026-08-14T00:00:00Z"
                                }
                                """.formatted(definitionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compositionDigest").value("A".repeat(64)))
                .andExpect(jsonPath("$.sources[0].ruleKey").value("grant.amount-parameters"))
                .andExpect(jsonPath("$.authorizedActions[0]").value("PREPARE"))
                .andExpect(jsonPath("$.authorizedActions[1]").value("PUBLISH"))
                .andExpect(jsonPath("$.ruleSet").doesNotExist())
                .andExpect(jsonPath("$.condition").doesNotExist());
    }

    @Test
    void publicationReceiptNeverExposesTheExecutableSnapshot() throws Exception {
        PublishedRuleSnapshot snapshot = new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                "snapshot-2", "desenv", "local", "praxis-api-quickstart", 2,
                "2026-08-14T00:00:00Z", "snapshot-1", "quickstart/1.0",
                "2026-08-14T00:00:00Z", null,
                List.of(new RuleSnapshotSource(
                        UUID.randomUUID().toString(), "grant.amount-parameters", 2, "D".repeat(64))),
                List.of(
                        new RuleSnapshotApproval("approval-1", "COMPOSITION_APPROVER", "approver-a",
                                "2026-08-13T23:55:00Z", "E".repeat(64)),
                        new RuleSnapshotApproval("approval-2", "COMPOSITION_APPROVER", "approver-b",
                                "2026-08-13T23:56:00Z", "F".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition(2));
        when(service.publish(any(), any(), any(), any(), any())).thenReturn(
                new DomainRuleSnapshotActivationResponse(
                        snapshot, "C".repeat(64), "head-etag-2", 2, "PUBLISHED"));

        mockMvc.perform(post(PATH + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promotedDefinitionId": "00000000-0000-0000-0000-000000000001",
                                  "validFromUtc": "2026-08-14T00:00:00Z",
                                  "expectedCompositionDigest": "%s"
                                }
                                """.formatted("A".repeat(64))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshotKey").value("snapshot-2"))
                .andExpect(jsonPath("$.ruleSetVersion").value(2))
                .andExpect(jsonPath("$.activationType").value("PUBLISHED"))
                .andExpect(jsonPath("$.snapshot").doesNotExist())
                .andExpect(jsonPath("$.ruleSet").doesNotExist());
    }

    @Test
    void mapsAChangedDigestToConflict() throws Exception {
        when(service.approve(any(), any(), any())).thenThrow(
                new StalePolicyStudioRuleSetCandidateException("changed"));

        mockMvc.perform(post(PATH + "/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promotedDefinitionId": "00000000-0000-0000-0000-000000000001",
                                  "validFromUtc": "2026-08-14T00:00:00Z",
                                  "expectedCompositionDigest": "%s"
                                }
                                """.formatted("A".repeat(64))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_RULESET_CANDIDATE"));
    }

    private TestingAuthenticationToken publisher() {
        return new TestingAuthenticationToken(
                "publisher", "ignored", RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER);
    }
}
