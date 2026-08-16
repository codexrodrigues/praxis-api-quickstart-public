package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.praxisplatform.config.dto.DomainRuleDefinitionRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Publishes the real local Rule Lab JSON Logic conditions into the governed Config read plane. */
@Configuration
@ConditionalOnBean(DomainRuleService.class)
@ConditionalOnProperty(
        prefix = "praxis.rule-lab.policy-studio-seed",
        name = "enabled",
        havingValue = "true")
class PolicyStudioRuleLabDefinitionSeed {

    private static final String SEED_ACTOR = "policy-studio-quickstart-seed";
    private static final String SERVICE_KEY = "praxis-api-quickstart";

    @Bean
    ApplicationRunner policyStudioRuleLabDefinitionSeedRunner(
            DomainRuleService domainRuleService,
            ObjectMapper objectMapper,
            @Value("${praxis.rule-lab.snapshot.tenant-id:desenv}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment:local}") String environment) {
        return args -> seed(domainRuleService, objectMapper, tenantId, environment);
    }

    void seed(DomainRuleService service, ObjectMapper objectMapper, String tenantId, String environment) {
        var seedPrincipal = new DomainRuleGovernancePrincipal(
                requireScope(tenantId, "tenant-id"), SEED_ACTOR, requireScope(environment, "environment"));
        var ruleSet = ExtraordinaryGrantRuleSetFactory.definition();
        Map<String, org.praxisplatform.rules.contract.DecisionSlot> slots = ruleSet.slots().stream()
                .collect(java.util.stream.Collectors.toMap(slot -> slot.slotKey(), slot -> slot));

        ExtraordinaryGrantRuleSetComposer.governedBindings(ruleSet).stream()
                .forEach(binding -> {
                    if (!service.definitions(
                            seedPrincipal.tenantId(),
                            seedPrincipal.environment(),
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
                            null), seedPrincipal);
                });
    }

    private String requireScope(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Policy Studio seed requires " + property);
        }
        return value.trim();
    }
}
