package com.example.praxis.apiquickstart.security;

import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import org.praxisplatform.uischema.capability.AvailabilityDecision;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Projects the Quickstart host security policies into operation discovery.
 *
 * <p>This provider is discovery metadata, not an authorization barrier. It keeps consumers aligned
 * with two policies enforced by Spring Security: mutating operations require an authenticated
 * principal, and HR analytics reads honor the configured demo/corporate access split.</p>
 */
@Component
public final class QuickstartResourceOperationAvailabilityProvider
        implements ResourceOperationAvailabilityProvider {

    static final String AUTHENTICATED_MUTATIONS_POLICY_ID = "quickstart-authenticated-mutations";
    static final String HR_ANALYTICS_POLICY_ID = "hr-analytics-access";

    private static final Set<String> HR_ANALYTICS_RESOURCE_KEYS = Set.of(
            "human-resources.vw-analytics-afastamentos",
            "human-resources.vw-analytics-folha-pagamento"
    );
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> NOMINAL_ANALYTICS_OPERATIONS = Set.of(
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

    private final boolean readOpen;

    public QuickstartResourceOperationAvailabilityProvider(
            @Value("${app.security.read-open:false}") boolean readOpen
    ) {
        this.readOpen = readOpen;
    }

    @Override
    public AvailabilityDecision evaluate(ResourceOperationAvailabilityContext context) {
        if (context == null) {
            return AvailabilityDecision.allowAll();
        }

        AvailabilityDecision mutationDecision = evaluateAuthenticatedMutation(context);
        if (!mutationDecision.allowed()) {
            return mutationDecision;
        }

        return evaluateHrAnalytics(context);
    }

    private AvailabilityDecision evaluateAuthenticatedMutation(
            ResourceOperationAvailabilityContext context
    ) {
        Object preferredMethod = context.metadata().get("preferredMethod");
        boolean mutating = preferredMethod != null
                && MUTATING_METHODS.contains(String.valueOf(preferredMethod).trim().toUpperCase());
        if (!mutating || hasAuthenticatedPrincipal()) {
            return AvailabilityDecision.allowAll();
        }
        return AvailabilityDecision.deny("authentication-required", Map.of(
                "policy", AUTHENTICATED_MUTATIONS_POLICY_ID,
                "preferredMethod", preferredMethod
        ));
    }

    private AvailabilityDecision evaluateHrAnalytics(ResourceOperationAvailabilityContext context) {
        if (!HR_ANALYTICS_RESOURCE_KEYS.contains(context.resourceKey())) {
            return AvailabilityDecision.allowAll();
        }

        if (readOpen) {
            return AvailabilityDecision.allow(Map.of(
                    "policy", HR_ANALYTICS_POLICY_ID,
                    "mode", "demo-read-open"
            ));
        }

        String requiredAuthority = requiredAnalyticsAuthority(context.operationId());
        if (requiredAuthority == null) {
            return AvailabilityDecision.allowAll();
        }
        if (hasAuthority(requiredAuthority)) {
            return AvailabilityDecision.allow(Map.of("policy", HR_ANALYTICS_POLICY_ID));
        }
        return AvailabilityDecision.deny("missing-authority", Map.of(
                "policy", HR_ANALYTICS_POLICY_ID,
                "blockedOperation", context.operationId(),
                "requiredAuthorities", List.of(requiredAuthority)
        ));
    }

    private String requiredAnalyticsAuthority(String operationId) {
        if ("statsComparison".equals(operationId)) {
            return HrAnalyticsAuthorities.AGGREGATE_READ;
        }
        return NOMINAL_ANALYTICS_OPERATIONS.contains(operationId)
                ? HrAnalyticsAuthorities.NOMINAL_READ
                : null;
    }

    private boolean hasAuthority(String requiredAuthority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> requiredAuthority.equals(authority.getAuthority()));
    }

    private boolean hasAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
