package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.praxisplatform.config.dto.DomainRuleDefinitionRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publishes the real local Rule Lab JSON Logic conditions into the governed Config read plane. */
@Configuration
@ConditionalOnBean(DomainRuleService.class)
@ConditionalOnProperty(
        prefix = "praxis.rule-lab.policy-studio-seed",
        name = "enabled",
        havingValue = "true")
class PolicyStudioRuleLabDefinitionSeed {

    private static final DomainRuleGovernancePrincipal SEED_PRINCIPAL =
            new DomainRuleGovernancePrincipal("desenv", "policy-studio-quickstart-seed", "local");
    private static final String SERVICE_KEY = "praxis-api-quickstart";

    @Bean
    ApplicationRunner policyStudioRuleLabDefinitionSeedRunner(
            DomainRuleService domainRuleService,
            ObjectMapper objectMapper) {
        return args -> seed(domainRuleService, objectMapper);
    }

    void seed(DomainRuleService service, ObjectMapper objectMapper) {
        var ruleSet = ExtraordinaryGrantRuleSetFactory.definition();
        Map<String, org.praxisplatform.rules.contract.DecisionSlot> slots = ruleSet.slots().stream()
                .collect(java.util.stream.Collectors.toMap(slot -> slot.slotKey(), slot -> slot));

        ExtraordinaryGrantRuleSetComposer.governedBindings(ruleSet).stream()
                .forEach(binding -> {
                    if (!service.definitions(
                            SEED_PRINCIPAL.tenantId(),
                            SEED_PRINCIPAL.environment(),
                            null,
                            null,
                            null,
                            binding.bindingKey()).isEmpty()) {
                        return;
                    }
                    var slot = slots.get(binding.slotKey());
                    var definition = objectMapper.valueToTree(Map.of(
                            "decisionStage", slot.stage().name(),
                            "cardinality", slot.cardinality().name(),
                            "overridePolicy", slot.overridePolicy().name(),
                            "aggregationPolicy", slot.aggregationPolicy().name(),
                            "bindingSource", binding.source().name(),
                            "falseDecision", binding.falseDecision().name(),
                            "falseReasonCode", binding.falseReasonCode(),
                            "requiredFactPaths", binding.requiredFactPaths()));
                    var parameters = objectMapper.valueToTree(Map.of(
                            "nullSemantics", "FAIL_CLOSED",
                            "operationKeys", java.util.List.of(ruleSet.ref().operationKey()),
                            "hostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                            "bindingOrder", binding.order()));
                    var governance = objectMapper.valueToTree(Map.of(
                            "lifecycleBoundary", "REFERENCE_DRAFT_ONLY",
                            "sourceKind", "QUICKSTART_RULE_LAB",
                            "sourceRuleSetVersion", ruleSet.ref().version(),
                            "requiredApprovals", java.util.List.of("policy-owner"),
                            "authorizedApprovers", java.util.List.of("policy-owner"),
                            "authorityChangeAllowed", false));

                    service.createDefinition(new DomainRuleDefinitionRequest(
                            binding.bindingKey(),
                            ruleSet.ref().version(),
                            "selection_eligibility",
                            "draft",
                            ruleSet.ref().boundedContextKey(),
                            ruleSet.ref().ruleSetKey(),
                            SERVICE_KEY,
                            "praxis-rules-engine",
                            "quickstart-rule-lab",
                            null,
                            null,
                            definition,
                            parameters,
                            binding.executor().expression(),
                            governance,
                            null), SEED_PRINCIPAL);
                });
    }
}
