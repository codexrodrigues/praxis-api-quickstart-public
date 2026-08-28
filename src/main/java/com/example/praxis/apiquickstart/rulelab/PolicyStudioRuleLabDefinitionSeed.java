package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.praxisplatform.config.dto.DomainRuleDefinitionRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Publishes the real local Rule Lab JSON Logic conditions into the governed Config read plane. */
@Configuration
@ConditionalOnProperty(
        prefix = "praxis.rule-lab.policy-studio-seed",
        name = "enabled",
        havingValue = "true")
class PolicyStudioRuleLabDefinitionSeed {

    private static final String SEED_ACTOR = "policy-studio-quickstart-seed";
    private static final String SERVICE_KEY = "praxis-api-quickstart";
    private static final String FACT_CATALOG_SCHEMA_VERSION = "praxis.domain-rule-fact-catalog.v1";
    private static final Map<String, FactSpec> FACTS = Map.ofEntries(
            fact("actor.permissions", "string-array", false, "Permissões do ator", "Actor permissions",
                    "Permissões efetivas usadas pelo guard protegido.", "Effective permissions used by the protected guard.",
                    "ExtraordinaryBenefitEvaluationService.actor.permissions", "SECRET", "OMIT"),
            fact("worker.status", "string", false, "Situação do trabalhador", "Worker status",
                    "Situação funcional usada na elegibilidade legal.", "Employment status used by legal eligibility.",
                    "JdbcExtraordinaryBenefitFactProvider.worker.status", "PERSONAL", "MASK"),
            fact("grant.hasDuplicate", "boolean", false, "Auxílio duplicado", "Duplicate grant",
                    "Indica conflito com auxílio já existente.", "Indicates a conflict with an existing grant.",
                    "JdbcExtraordinaryBenefitFactProvider.grant.hasDuplicate", "SENSITIVE", "MASK"),
            fact("program.active", "boolean", false, "Programa ativo", "Active program",
                    "Indica se o programa é aplicável.", "Indicates whether the program is applicable.",
                    "JdbcExtraordinaryBenefitFactProvider.program.active", "NON_SENSITIVE", "NONE"),
            fact("payment.requestedDate", "date", false, "Data solicitada", "Requested date",
                    "Data pretendida para pagamento.", "Requested payment date.",
                    "ExtraordinaryBenefitEvaluationService.payment.requestedDate", "PERSONAL", "MASK"),
            fact("payment.allowedDates", "date-array", false, "Datas permitidas", "Allowed dates",
                    "Calendário permitido para pagamento.", "Allowed payment calendar.",
                    "JdbcExtraordinaryBenefitFactProvider.payment.allowedDates", "NON_SENSITIVE", "NONE"),
            fact("request.requestedAmount", "number", false, "Valor solicitado", "Requested amount",
                    "Valor monetário solicitado pelo trabalhador.", "Monetary amount requested by the worker.",
                    "ExtraordinaryBenefitEvaluationService.request.requestedAmount", "SENSITIVE", "MASK"),
            fact("program.maxAmount", "number", false, "Limite do programa", "Program limit",
                    "Valor máximo permitido pelo programa.", "Maximum amount allowed by the program.",
                    "JdbcExtraordinaryBenefitFactProvider.program.maxAmount", "NON_SENSITIVE", "NONE"),
            fact("budget.availableAmount", "number", false, "Orçamento disponível", "Available budget",
                    "Saldo disponível para concessão do auxílio.", "Budget available for granting the benefit.",
                    "JdbcExtraordinaryBenefitFactProvider.budget.availableAmount", "SENSITIVE", "MASK"));

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
                    var existingDefinitions = service.definitions(
                            seedPrincipal.tenantId(), seedPrincipal.environment(),
                            null, null, null, binding.bindingKey());
                    if (existingDefinitions.stream().anyMatch(this::hasCurrentFactCatalog)) {
                        return;
                    }
                    int nextVersion = existingDefinitions.stream()
                            .mapToInt(org.praxisplatform.config.dto.DomainRuleDefinitionResponse::version)
                            .max()
                            .orElse(0) + 1;
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
                    ((ObjectNode) definition).set("factCatalog", factCatalog(objectMapper, binding.requiredFactPaths()));
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
                            nextVersion,
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

    private boolean hasCurrentFactCatalog(org.praxisplatform.config.dto.DomainRuleDefinitionResponse definition) {
        return definition.definition() != null
                && FACT_CATALOG_SCHEMA_VERSION.equals(
                definition.definition().path("factCatalog").path("schemaVersion").asText());
    }

    private ObjectNode factCatalog(ObjectMapper objectMapper, List<String> requiredFactPaths) {
        ObjectNode catalog = objectMapper.createObjectNode();
        catalog.put("schemaVersion", FACT_CATALOG_SCHEMA_VERSION);
        ArrayNode facts = catalog.putArray("facts");
        requiredFactPaths.forEach(path -> facts.add(factNode(objectMapper, requireFact(path))));
        return catalog;
    }

    private ObjectNode factNode(ObjectMapper objectMapper, FactSpec fact) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("path", fact.path());
        node.put("valueType", fact.valueType());
        node.put("nullable", fact.nullable());
        node.putObject("labels").put("pt-BR", fact.labelPt()).put("en-US", fact.labelEn());
        node.putObject("descriptions").put("pt-BR", fact.descriptionPt()).put("en-US", fact.descriptionEn());
        node.put("providerRef", fact.providerRef());
        node.putArray("evidenceRefs").add("ExtraordinaryGrantRuleSetFactory.java");
        node.put("sensitivity", fact.sensitivity());
        node.put("redaction", fact.redaction());
        return node;
    }

    private FactSpec requireFact(String path) {
        FactSpec fact = FACTS.get(path);
        if (fact == null) {
            throw new IllegalStateException("Policy Studio seed has no governed fact metadata for " + path);
        }
        return fact;
    }

    private static Map.Entry<String, FactSpec> fact(
            String path,
            String valueType,
            boolean nullable,
            String labelPt,
            String labelEn,
            String descriptionPt,
            String descriptionEn,
            String providerRef,
            String sensitivity,
            String redaction) {
        return Map.entry(path, new FactSpec(path, valueType, nullable, labelPt, labelEn,
                descriptionPt, descriptionEn, providerRef, sensitivity, redaction));
    }

    private record FactSpec(
            String path,
            String valueType,
            boolean nullable,
            String labelPt,
            String labelEn,
            String descriptionPt,
            String descriptionEn,
            String providerRef,
            String sensitivity,
            String redaction) {
    }

    private String requireScope(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Policy Studio seed requires " + property);
        }
        return value.trim();
    }
}
