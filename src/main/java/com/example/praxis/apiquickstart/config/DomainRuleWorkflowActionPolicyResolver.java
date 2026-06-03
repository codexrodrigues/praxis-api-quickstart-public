package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.praxisplatform.config.dto.DomainRuleMaterializationResponse;
import org.praxisplatform.config.service.DomainRuleService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Consumes governed workflow-action materializations from the hosted config starter.
 *
 * <p>The canonical decision remains in {@code praxis-config-starter}; this resolver only proves
 * that an operational action can honor an applied semantic decision at runtime.</p>
 */
@Component
public class DomainRuleWorkflowActionPolicyResolver {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_ENVIRONMENT = "dev";
    private static final String TARGET_LAYER = "workflow_action";
    private static final String TARGET_ARTIFACT_TYPE = "resource-workflow-action";
    private static final String APPLIED_STATUS = "applied";

    private final ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    public DomainRuleWorkflowActionPolicyResolver(ObjectProvider<DomainRuleService> domainRuleServiceProvider) {
        this.domainRuleServiceProvider = domainRuleServiceProvider;
    }

    public Optional<DomainRuleWorkflowActionPolicy> resolveAppliedPolicy(String targetArtifactKey) {
        if (!StringUtils.hasText(targetArtifactKey)) {
            return Optional.empty();
        }
        DomainRuleService domainRuleService = domainRuleServiceProvider.getIfAvailable();
        if (domainRuleService == null) {
            return Optional.empty();
        }
        try {
            return domainRuleService.materializations(
                            currentHeader("X-Tenant-ID").orElse(DEFAULT_TENANT),
                            currentHeader("X-Env").orElse(DEFAULT_ENVIRONMENT),
                            null,
                            TARGET_LAYER,
                            TARGET_ARTIFACT_TYPE,
                            targetArtifactKey.trim(),
                            APPLIED_STATUS)
                    .stream()
                    .filter(materialization -> materialization.materializedPayload() != null)
                    .filter(this::isWorkflowActionPolicy)
                    .max(Comparator.comparing(this::effectiveAppliedAt))
                    .flatMap(this::toWorkflowActionPolicy);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private boolean isWorkflowActionPolicy(DomainRuleMaterializationResponse materialization) {
        return "workflow_action_policy".equals(materialization.materializedPayload().path("kind").asText(null));
    }

    private Optional<DomainRuleWorkflowActionPolicy> toWorkflowActionPolicy(
            DomainRuleMaterializationResponse materialization) {
        JsonNode payload = materialization.materializedPayload();
        JsonNode workflowAction = payload.path("workflowAction");
        JsonNode availabilityPolicy = payload.path("availabilityPolicy");
        if (!workflowAction.isObject() || !availabilityPolicy.isObject()) {
            return Optional.empty();
        }
        return Optional.of(new DomainRuleWorkflowActionPolicy(
                materialization.targetArtifactKey(),
                text(workflowAction, "resourceKey"),
                text(workflowAction, "actionId"),
                strings(availabilityPolicy.path("requiredStates")),
                firstText(
                        availabilityPolicy.path("message").asText(null),
                        payload.path("message").asText(null),
                        "Workflow action blocked by published semantic decision")
        ));
    }

    private Instant effectiveAppliedAt(DomainRuleMaterializationResponse materialization) {
        if (materialization.appliedAt() != null) {
            return materialization.appliedAt();
        }
        if (materialization.updatedAt() != null) {
            return materialization.updatedAt();
        }
        if (materialization.createdAt() != null) {
            return materialization.createdAt();
        }
        return Instant.EPOCH;
    }

    private Optional<String> currentHeader(String headerName) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        String value = attributes.getRequest().getHeader(headerName);
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private static String text(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        node.elements().forEachRemaining(value -> {
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                values.add(value.asText().trim());
            }
        });
        return values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
