package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.praxisplatform.config.dto.DomainRuleMaterializationResponse;
import org.praxisplatform.config.service.DomainRuleService;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
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
 * Bridges applied shared-rule materializations into option-source runtime policy.
 *
 * <p>The canonical decision and materialization state remains in {@code praxis-config-starter};
 * this quickstart resolver only consumes the hosted {@code domain-rules} surface to demonstrate
 * how a published semantic decision can govern a real lookup.</p>
 */
@Component
public class DomainRuleOptionSourcePolicyResolver {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_ENVIRONMENT = "dev";
    private static final String TARGET_LAYER = "option_source";
    private static final String TARGET_ARTIFACT_TYPE = "resource-option-source";
    private static final String APPLIED_STATUS = "applied";

    private final ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    public DomainRuleOptionSourcePolicyResolver(ObjectProvider<DomainRuleService> domainRuleServiceProvider) {
        this.domainRuleServiceProvider = domainRuleServiceProvider;
    }

    public Optional<LookupSelectionPolicy> resolveAppliedSelectionPolicy(String targetArtifactKey) {
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
                    .filter(this::isLookupSelectionPolicy)
                    .max(Comparator.comparing(this::effectiveAppliedAt))
                    .flatMap(this::toSelectionPolicy);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private boolean isLookupSelectionPolicy(DomainRuleMaterializationResponse materialization) {
        return "lookup_selection_policy".equals(materialization.materializedPayload().path("kind").asText(null));
    }

    private Optional<LookupSelectionPolicy> toSelectionPolicy(DomainRuleMaterializationResponse materialization) {
        JsonNode selectionPolicy = materialization.materializedPayload().path("selectionPolicy");
        if (!selectionPolicy.isObject()) {
            return Optional.empty();
        }
        return Optional.of(new LookupSelectionPolicy(
                text(selectionPolicy, "selectablePropertyPath"),
                text(selectionPolicy, "statusPropertyPath"),
                strings(selectionPolicy.path("allowedStatuses")),
                strings(selectionPolicy.path("blockedStatuses")),
                selectionPolicy.path("allowRetainInvalidExistingValue").asBoolean(true),
                text(selectionPolicy, "disabledReasonTemplate"),
                text(selectionPolicy, "validationMessageTemplate")
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
