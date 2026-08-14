package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.rules.contract.DecisionBinding;

class ExtraordinaryGrantRuleSetComposerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void materializesEveryGovernedJsonBindingFromItsExactApprovedDefinition() throws Exception {
        List<DomainRuleDefinitionResponse> sources = approvedSources();
        int changedIndex = 5;
        DomainRuleDefinitionResponse changed = sources.get(changedIndex);
        var changedCondition = JSON.readTree("{\"<=\":[{\"var\":\"request.requestedAmount\"},2500]}");
        sources.set(changedIndex, copy(changed, "approved", changed.resourceKey(), changedCondition));

        var candidate = ExtraordinaryGrantRuleSetComposer.compose(3, sources);

        assertThat(candidate.ruleSet().ref().version()).isEqualTo(3);
        assertThat(candidate.sourceDefinitionIds()).containsExactlyElementsOf(
                sources.stream().map(DomainRuleDefinitionResponse::id).toList());
        assertThat(candidate.ruleSet().bindings()).filteredOn(binding ->
                binding.bindingKey().equals("grant.amount-parameters"))
                .singleElement()
                .extracting(binding -> binding.executor().expression())
                .isEqualTo(changedCondition);
        assertThat(candidate.ruleSet().bindings()).filteredOn(binding ->
                binding.bindingKey().equals("grant.effect-plan"))
                .singleElement()
                .satisfies(binding -> assertThat(binding.executor().implementationKey())
                        .isEqualTo("benefits:extraordinary-grant-effect-plan"));
    }

    @Test
    void rejectsIncompleteUnapprovedOrCrossRuleSetSources() {
        List<DomainRuleDefinitionResponse> incomplete = approvedSources();
        incomplete.removeLast();
        assertThatThrownBy(() -> ExtraordinaryGrantRuleSetComposer.compose(1, incomplete))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly cover");

        List<DomainRuleDefinitionResponse> unapproved = approvedSources();
        DomainRuleDefinitionResponse first = unapproved.getFirst();
        unapproved.set(0, copy(first, "draft", first.resourceKey(), first.condition()));
        assertThatThrownBy(() -> ExtraordinaryGrantRuleSetComposer.compose(1, unapproved))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved or active");

        List<DomainRuleDefinitionResponse> crossRuleSet = approvedSources();
        DomainRuleDefinitionResponse second = crossRuleSet.get(1);
        crossRuleSet.set(1, copy(second, second.status(), "another-ruleset", second.condition()));
        assertThatThrownBy(() -> ExtraordinaryGrantRuleSetComposer.compose(1, crossRuleSet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host RuleSet boundary");
    }

    @Test
    void exposesTheSameSevenBindingsUsedByThePolicyStudioSeed() {
        assertThat(ExtraordinaryGrantRuleSetComposer.governedBindings(
                ExtraordinaryGrantRuleSetFactory.definition()))
                .extracting(DecisionBinding::bindingKey)
                .containsExactly(
                        "request.authorization-integrity",
                        "worker.legal-eligibility",
                        "grant.duplicate-conflict",
                        "program.applicability",
                        "payment.calendar-policy",
                        "grant.amount-parameters",
                        "budget.availability");
    }

    private static List<DomainRuleDefinitionResponse> approvedSources() {
        var ruleSet = ExtraordinaryGrantRuleSetFactory.definition();
        List<DomainRuleDefinitionResponse> sources = new ArrayList<>();
        for (DecisionBinding binding : ExtraordinaryGrantRuleSetComposer.governedBindings(ruleSet)) {
            var parameters = JSON.createObjectNode()
                    .put("hostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION);
            sources.add(new DomainRuleDefinitionResponse(
                    UUID.nameUUIDFromBytes(binding.bindingKey().getBytes(StandardCharsets.UTF_8)),
                    "desenv", "local", binding.bindingKey(), 2, "selection_eligibility", "approved",
                    ruleSet.ref().boundedContextKey(), ruleSet.ref().ruleSetKey(),
                    ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                    "workforce-benefits-owner", "rule-platform-steward", null, null,
                    JSON.createObjectNode(), parameters, binding.executor().expression(),
                    JSON.createObjectNode(), JSON.createObjectNode(), "authenticated", "publisher",
                    "reviewer", Instant.parse("2026-08-13T10:00:00Z"),
                    Instant.parse("2026-08-13T10:05:00Z"), Instant.parse("2026-08-13T10:05:00Z"), null));
        }
        return sources;
    }

    private static DomainRuleDefinitionResponse copy(
            DomainRuleDefinitionResponse source,
            String status,
            String resourceKey,
            com.fasterxml.jackson.databind.JsonNode condition) {
        return new DomainRuleDefinitionResponse(
                source.id(), source.tenantId(), source.environment(), source.ruleKey(), source.version(),
                source.ruleType(), status, source.contextKey(), resourceKey, source.serviceKey(),
                source.semanticOwner(), source.steward(), source.sourceReleaseId(), source.sourceChangeSetId(),
                source.definition(), source.parameters(), condition, source.governance(), source.validationResult(),
                source.createdByType(), source.createdBy(), source.approvedBy(), source.createdAt(),
                source.updatedAt(), source.approvedAt(), source.activatedAt());
    }
}
