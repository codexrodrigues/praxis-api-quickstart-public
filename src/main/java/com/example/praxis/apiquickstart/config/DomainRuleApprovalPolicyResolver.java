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
 * Consumes governed approval-policy materializations from the hosted config starter.
 *
 * <p>The canonical approval decision remains in {@code praxis-config-starter}; this resolver only
 * proves that an operational action can honor an applied semantic approval gate at runtime.</p>
 */
@Component
public class DomainRuleApprovalPolicyResolver {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_ENVIRONMENT = "dev";
    private static final String TARGET_LAYER = "approval_policy";
    private static final String TARGET_ARTIFACT_TYPE = "resource-action-approval";
    private static final String APPLIED_STATUS = "applied";

    private final ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    public DomainRuleApprovalPolicyResolver(ObjectProvider<DomainRuleService> domainRuleServiceProvider) {
        this.domainRuleServiceProvider = domainRuleServiceProvider;
    }

    public Optional<DomainRuleApprovalPolicy> resolveAppliedPolicy(String targetArtifactKey) {
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
                    .filter(this::isApprovalPolicy)
                    .max(Comparator.comparing(this::effectiveAppliedAt))
                    .flatMap(this::toApprovalPolicy);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private boolean isApprovalPolicy(DomainRuleMaterializationResponse materialization) {
        return "approval_policy".equals(materialization.materializedPayload().path("kind").asText(null));
    }

    private Optional<DomainRuleApprovalPolicy> toApprovalPolicy(DomainRuleMaterializationResponse materialization) {
        JsonNode payload = materialization.materializedPayload();
        JsonNode resourceAction = payload.path("resourceAction");
        JsonNode approvalPolicy = payload.path("approvalPolicy");
        if (!resourceAction.isObject() || !approvalPolicy.isObject()) {
            return Optional.empty();
        }
        return Optional.of(new DomainRuleApprovalPolicy(
                materialization.targetArtifactKey(),
                text(resourceAction, "resourceKey"),
                text(resourceAction, "actionId"),
                strings(approvalPolicy.path("requiredApprovals")),
                strings(approvalPolicy.path("approvalGroups")),
                text(approvalPolicy, "approverContext"),
                firstText(
                        approvalPolicy.path("message").asText(null),
                        payload.path("message").asText(null),
                        "Action requires approval by published semantic decision")
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
