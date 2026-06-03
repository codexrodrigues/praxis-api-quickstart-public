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
 * Bridges applied shared-rule materializations into backend validation runtime policy.
 *
 * <p>The canonical decision and materialization state remains in {@code praxis-config-starter};
 * this quickstart resolver only consumes the hosted {@code domain-rules} surface to prove that a
 * published semantic decision can govern command-side validation.</p>
 */
@Component
public class DomainRuleBackendValidationPolicyResolver {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_ENVIRONMENT = "dev";
    private static final String TARGET_LAYER = "backend_validation";
    private static final String TARGET_ARTIFACT_TYPE = "resource-validation";
    private static final String APPLIED_STATUS = "applied";

    private final ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    public DomainRuleBackendValidationPolicyResolver(ObjectProvider<DomainRuleService> domainRuleServiceProvider) {
        this.domainRuleServiceProvider = domainRuleServiceProvider;
    }

    public Optional<DomainRuleBackendValidationPolicy> resolveAppliedPolicy(String targetArtifactKey) {
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
                    .filter(this::isResourceValidationPolicy)
                    .max(Comparator.comparing(this::effectiveAppliedAt))
                    .flatMap(this::toValidationPolicy);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private boolean isResourceValidationPolicy(DomainRuleMaterializationResponse materialization) {
        return "resource_validation_policy".equals(materialization.materializedPayload().path("kind").asText(null));
    }

    private Optional<DomainRuleBackendValidationPolicy> toValidationPolicy(DomainRuleMaterializationResponse materialization) {
        JsonNode validationPolicy = materialization.materializedPayload().path("validationPolicy");
        if (!validationPolicy.isObject()) {
            return Optional.empty();
        }
        JsonNode parameters = validationPolicy.path("parameters");
        List<String> blockedStatuses = strings(parameters.path("blockedStatuses"));
        if (blockedStatuses.isEmpty()) {
            blockedStatuses = conditionStatuses(validationPolicy);
        }
        if (blockedStatuses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DomainRuleBackendValidationPolicy(
                materialization.targetArtifactKey(),
                textOrDefault(parameters, "referenceResourceKey", "procurement.suppliers"),
                textOrDefault(parameters, "referenceField", "supplierId"),
                textOrDefault(parameters, "statusPropertyPath", "status"),
                blockedStatuses,
                firstText(
                        validationPolicy.path("validationMessageTemplate").asText(null),
                        parameters.path("validationMessageTemplate").asText(null),
                        "Referenced resource is blocked by published backend validation policy")
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

    private static List<String> conditionStatuses(JsonNode validationPolicy) {
        JsonNode values = validationPolicy.path("condition").path("in");
        if (!values.isArray() || values.size() < 2) {
            return List.of();
        }
        return strings(values.get(1));
    }

    private static String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
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
