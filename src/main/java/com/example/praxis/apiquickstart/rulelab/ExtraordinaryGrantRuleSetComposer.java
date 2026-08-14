package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleExecutorType;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/**
 * Materializes the host-owned RuleSet graph from exact approved Policy Studio definitions.
 *
 * <p>The Config control plane signs the complete graph and its source hashes. This host composer
 * closes the preceding derivation boundary: every standalone JSON Logic policy binding must come
 * from one approved definition in the same RuleSet identity. Java and fixed composition bindings
 * remain host-owned and continue to be admitted by the executable/planning registries.</p>
 */
final class ExtraordinaryGrantRuleSetComposer {
    private static final Set<String> PUBLISHABLE_STATUSES = Set.of("approved", "active");

    private ExtraordinaryGrantRuleSetComposer() {
    }

    static SnapshotCandidate compose(
            int ruleSetVersion,
            Collection<DomainRuleDefinitionResponse> sourceDefinitions) {
        RuleSetDefinition baseline = ExtraordinaryGrantRuleSetFactory.definition(ruleSetVersion);
        Map<String, DomainRuleDefinitionResponse> sources = indexSources(sourceDefinitions);
        List<String> governedKeys = governedBindings(baseline).stream()
                .map(DecisionBinding::bindingKey)
                .toList();
        if (!sources.keySet().equals(Set.copyOf(governedKeys))) {
            throw new IllegalArgumentException(
                    "Approved source definitions must exactly cover the host-governed JSON Logic bindings");
        }

        Map<String, JsonNode> conditions = new LinkedHashMap<>();
        for (String bindingKey : governedKeys) {
            DomainRuleDefinitionResponse source = sources.get(bindingKey);
            validateSource(source, baseline);
            conditions.put(bindingKey, source.condition());
        }
        RuleSetDefinition materialized = withConditions(baseline, conditions);
        List<UUID> sourceIds = governedKeys.stream().map(key -> sources.get(key).id()).toList();
        return new SnapshotCandidate(materialized, sourceIds);
    }

    static RuleSetDefinition withCondition(
            RuleSetDefinition baseline,
            String bindingKey,
            JsonNode condition) {
        return withConditions(baseline, Map.of(requireText(bindingKey, "bindingKey"), requireCondition(condition)));
    }

    static List<DecisionBinding> governedBindings(RuleSetDefinition ruleSet) {
        return ruleSet.bindings().stream()
                .filter(binding -> binding.bindingKey().equals(binding.slotKey()))
                .filter(binding -> binding.executor().type() == RuleExecutorType.JSON_LOGIC)
                .toList();
    }

    private static RuleSetDefinition withConditions(
            RuleSetDefinition baseline,
            Map<String, JsonNode> conditions) {
        Objects.requireNonNull(baseline, "baseline is required");
        Set<String> replaceable = governedBindings(baseline).stream()
                .map(DecisionBinding::bindingKey)
                .collect(Collectors.toSet());
        if (!replaceable.containsAll(conditions.keySet())) {
            throw new IllegalArgumentException("A condition targets a binding outside the governed host projection");
        }
        List<DecisionBinding> bindings = baseline.bindings().stream().map(binding -> {
            JsonNode condition = conditions.get(binding.bindingKey());
            if (condition == null) return binding;
            return new DecisionBinding(
                    binding.bindingKey(), binding.slotKey(), binding.source(), binding.compositionPolicy(),
                    RuleExecutorRef.jsonLogic(requireCondition(condition)), binding.dependsOn(), binding.order(),
                    binding.enabled(), binding.falseDecision(), binding.falseReasonCode(), binding.requiredFactPaths());
        }).toList();
        return new RuleSetDefinition(
                baseline.ref(), baseline.availableRoots(), baseline.slots(), bindings,
                baseline.compatibility(), baseline.failPolicy());
    }

    private static Map<String, DomainRuleDefinitionResponse> indexSources(
            Collection<DomainRuleDefinitionResponse> sourceDefinitions) {
        if (sourceDefinitions == null || sourceDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Approved source definitions are required");
        }
        Map<String, DomainRuleDefinitionResponse> indexed = new LinkedHashMap<>();
        for (DomainRuleDefinitionResponse source : sourceDefinitions) {
            if (source == null || source.id() == null) {
                throw new IllegalArgumentException("Every source definition requires an immutable ID");
            }
            String key = requireText(source.ruleKey(), "source ruleKey");
            if (indexed.putIfAbsent(key, source) != null) {
                throw new IllegalArgumentException("Source definition ruleKeys must be distinct");
            }
        }
        return indexed;
    }

    private static void validateSource(
            DomainRuleDefinitionResponse source,
            RuleSetDefinition baseline) {
        if (!PUBLISHABLE_STATUSES.contains(source.status())) {
            throw new IllegalArgumentException("Every source definition must be approved or active");
        }
        if (!baseline.ref().boundedContextKey().equals(source.contextKey())
                || !baseline.ref().ruleSetKey().equals(source.resourceKey())
                || !ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY.equals(source.serviceKey())) {
            throw new IllegalArgumentException("Source definition does not belong to this host RuleSet boundary");
        }
        if (!ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION.equals(
                source.parameters() == null ? null : source.parameters().path("hostContractVersion").asText(null))) {
            throw new IllegalArgumentException("Source definition host contract is incompatible");
        }
        requireCondition(source.condition());
    }

    private static JsonNode requireCondition(JsonNode condition) {
        if (condition == null || !condition.isObject() || condition.isEmpty()) {
            throw new IllegalArgumentException("Every governed JSON Logic source requires an object condition");
        }
        return condition;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    record SnapshotCandidate(RuleSetDefinition ruleSet, List<UUID> sourceDefinitionIds) {
        SnapshotCandidate {
            Objects.requireNonNull(ruleSet, "ruleSet is required");
            sourceDefinitionIds = List.copyOf(sourceDefinitionIds);
        }
    }
}
