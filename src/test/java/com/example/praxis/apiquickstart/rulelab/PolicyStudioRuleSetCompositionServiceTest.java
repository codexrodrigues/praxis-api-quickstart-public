package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestRequest;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestResponse;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.config.dto.DomainRuleSnapshotHeadStatusResponse;
import org.praxisplatform.config.contract.RuleSetCompositionAction;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateCommand;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;

class PolicyStudioRuleSetCompositionServiceTest {
    private static final DomainRuleGovernancePrincipal PRINCIPAL =
            new DomainRuleGovernancePrincipal("desenv", "publisher", "local");
    private static final String VALID_FROM = "2026-08-14T00:00:00Z";

    private DomainRuleService domainRules;
    private DomainRuleSnapshotService snapshots;
    private PolicyStudioRuleSetCompositionService service;

    @BeforeEach
    void setUp() {
        domainRules = mock(DomainRuleService.class);
        snapshots = mock(DomainRuleSnapshotService.class);
        service = new PolicyStudioRuleSetCompositionService(domainRules, snapshots);
    }

    @Test
    void composesThePromotedDefinitionWithEveryOtherLatestApprovedSource() {
        List<DomainRuleDefinitionResponse> definitions = approvedSources();
        DomainRuleDefinitionResponse oldFirst = definitions.get(0);
        DomainRuleDefinitionResponse promoted = source(
                oldFirst.ruleKey(), 2, oldFirst.condition(), "approved");
        definitions.add(promoted);
        when(domainRules.definitions(
                "desenv", "local", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                null, null, null)).thenReturn(definitions);
        when(snapshots.findHeadStatus("desenv", "local", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY))
                .thenReturn(java.util.Optional.of(new DomainRuleSnapshotHeadStatusResponse(
                        ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                        "snapshot-v1", 1, 1, 1, "head-etag", true, "READY")));
        when(snapshots.prepareCompositionManifest(any(), eq("desenv"), eq("local")))
                .thenReturn(new DomainRuleCompositionManifestResponse(
                        "praxis-rule-composition-manifest/v1", "A".repeat(64), "B".repeat(64),
                        mock(JsonNode.class)));

        var response = service.prepare(
                ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                new RuleSetCompositionCandidateRequest(promoted.id(), VALID_FROM, null),
                PRINCIPAL,
                List.of(RuleSetCompositionAction.PREPARE, RuleSetCompositionAction.PUBLISH));

        assertThat(response.ruleSetVersion()).isEqualTo(2);
        assertThat(response.currentHeadEtag()).isEqualTo("head-etag");
        assertThat(response.sources()).hasSize(7);
        assertThat(response.authorizedActions()).containsExactly(
                RuleSetCompositionAction.PREPARE,
                RuleSetCompositionAction.PUBLISH);
        assertThat(response.sources()).anySatisfy(source -> {
            assertThat(source.definitionId()).isEqualTo(promoted.id());
            assertThat(source.version()).isEqualTo(2);
        });
        ArgumentCaptor<DomainRuleCompositionManifestRequest> request =
                ArgumentCaptor.forClass(DomainRuleCompositionManifestRequest.class);
        verify(snapshots).prepareCompositionManifest(request.capture(), eq("desenv"), eq("local"));
        assertThat(request.getValue().sourceDefinitionIds()).contains(promoted.id()).doesNotContain(oldFirst.id());
        assertThat(request.getValue().ruleSet().ref().version()).isEqualTo(2);
    }

    @Test
    void rejectsACommandWhenTheRecomposedDigestDiffersFromTheInspectedCandidate() {
        List<DomainRuleDefinitionResponse> definitions = approvedSources();
        DomainRuleDefinitionResponse promoted = definitions.get(0);
        when(domainRules.definitions(any(), any(), any(), any(), any(), any())).thenReturn(definitions);
        when(snapshots.findHeadStatus(any(), any(), any())).thenReturn(java.util.Optional.empty());
        when(snapshots.prepareCompositionManifest(any(), any(), any()))
                .thenReturn(new DomainRuleCompositionManifestResponse(
                        "praxis-rule-composition-manifest/v1", "A".repeat(64), "B".repeat(64),
                        mock(JsonNode.class)));

        var command = new RuleSetCompositionCandidateCommand(
                promoted.id(), VALID_FROM, null, "C".repeat(64));

        assertThatThrownBy(() -> service.approve(
                ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY, command, PRINCIPAL))
                .isInstanceOf(StalePolicyStudioRuleSetCandidateException.class)
                .hasMessageContaining("changed after inspection");
        verify(snapshots, never()).approveComposition(any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenOneGovernedBindingHasNoApprovedSource() {
        List<DomainRuleDefinitionResponse> definitions = approvedSources();
        DomainRuleDefinitionResponse promoted = definitions.remove(0);
        definitions.remove(0);
        definitions.add(promoted);
        when(domainRules.definitions(any(), any(), any(), any(), any(), any())).thenReturn(definitions);

        assertThatThrownBy(() -> service.prepare(
                ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                new RuleSetCompositionCandidateRequest(promoted.id(), VALID_FROM, null),
                PRINCIPAL,
                List.of(RuleSetCompositionAction.PREPARE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Complete approved RuleSet coverage");
        verify(snapshots, never()).prepareCompositionManifest(any(), any(), any());
    }

    private List<DomainRuleDefinitionResponse> approvedSources() {
        List<DomainRuleDefinitionResponse> result = new ArrayList<>();
        ExtraordinaryGrantRuleSetComposer.governedBindings(ExtraordinaryGrantRuleSetFactory.definition())
                .forEach(binding -> result.add(source(
                        binding.bindingKey(), 1, binding.executor().expression(), "approved")));
        return result;
    }

    private DomainRuleDefinitionResponse source(
            String ruleKey,
            int version,
            JsonNode condition,
            String status) {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");
        return new DomainRuleDefinitionResponse(
                UUID.randomUUID(), "desenv", "local", ruleKey, version,
                "selection_eligibility", status,
                ExtraordinaryGrantRuleSetFactory.definition().ref().boundedContextKey(),
                ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                "praxis-rules-engine", "quickstart-rule-lab", null, null,
                null, com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("hostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION),
                condition,
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("lifecycleBoundary", "REFERENCE_DRAFT_ONLY"),
                null, "authenticated", "author", "approver", now, now, now, null);
    }
}
