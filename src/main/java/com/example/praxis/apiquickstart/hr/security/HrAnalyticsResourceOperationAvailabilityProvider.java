package com.example.praxis.apiquickstart.hr.security;

import org.praxisplatform.uischema.capability.AvailabilityDecision;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Publishes the same HR analytics access split enforced by the host security chain.
 *
 * <p>The provider is discovery metadata, not an authorization barrier. It lets Praxis authoring
 * and runtimes distinguish aggregate comparison from nominal rows without probing protected
 * endpoints or reproducing Spring Security rules.</p>
 */
@Component
public final class HrAnalyticsResourceOperationAvailabilityProvider
        implements ResourceOperationAvailabilityProvider {

    static final Set<String> RESOURCE_KEYS = Set.of(
            "human-resources.vw-analytics-afastamentos",
            "human-resources.vw-analytics-folha-pagamento"
    );
    static final String POLICY_ID = "hr-analytics-access";

    private static final Set<String> NOMINAL_OPERATIONS = Set.of(
            "view",
            "byId",
            "all",
            "filter",
            "cursor",
            "options",
            "optionSources",
            "export",
            "statsGroupBy",
            "statsTimeSeries",
            "statsDistribution"
    );

    @Override
    public AvailabilityDecision evaluate(ResourceOperationAvailabilityContext context) {
        if (context == null || !RESOURCE_KEYS.contains(context.resourceKey())) {
            return AvailabilityDecision.allowAll();
        }

        String requiredAuthority = requiredAuthority(context.operationId());
        if (requiredAuthority == null) {
            return AvailabilityDecision.allowAll();
        }

        if (hasAuthority(requiredAuthority)) {
            return AvailabilityDecision.allow(Map.of("policy", POLICY_ID));
        }
        return AvailabilityDecision.deny("missing-authority", Map.of(
                "policy", POLICY_ID,
                "blockedOperation", context.operationId(),
                "requiredAuthorities", List.of(requiredAuthority)
        ));
    }

    private String requiredAuthority(String operationId) {
        if ("statsComparison".equals(operationId)) {
            return HrAnalyticsAuthorities.AGGREGATE_READ;
        }
        return NOMINAL_OPERATIONS.contains(operationId)
                ? HrAnalyticsAuthorities.NOMINAL_READ
                : null;
    }

    private boolean hasAuthority(String requiredAuthority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> requiredAuthority.equals(authority.getAuthority()));
    }
}
