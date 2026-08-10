package com.example.praxis.apiquickstart.security;

import org.praxisplatform.uischema.action.ActionAvailabilityContext;
import org.praxisplatform.uischema.action.ActionAvailabilityRule;
import org.praxisplatform.uischema.action.ActionDefinition;
import org.praxisplatform.uischema.capability.AvailabilityDecision;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Projects the host's authenticated-mutation policy into workflow-action discovery.
 *
 * <p>Spring Security already protects mutating endpoints. This rule makes the same policy
 * observable before a client opens a command form, preventing a public read-only proof from
 * advertising actions that its anonymous session cannot execute.</p>
 */
@Component
@Order(50)
public final class QuickstartAuthenticatedMutationActionAvailabilityRule
        implements ActionAvailabilityRule {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public AvailabilityDecision evaluate(
            ActionDefinition definition,
            ActionAvailabilityContext context
    ) {
        String method = definition == null || definition.operation() == null
                ? null
                : definition.operation().method();
        if (method == null || !MUTATING_METHODS.contains(method.trim().toUpperCase())) {
            return AvailabilityDecision.allowAll();
        }
        if (hasAuthenticatedPrincipal()) {
            return AvailabilityDecision.allowAll();
        }
        return AvailabilityDecision.deny("authentication-required", Map.of(
                "policy", QuickstartResourceOperationAvailabilityProvider.AUTHENTICATED_MUTATIONS_POLICY_ID,
                "preferredMethod", method.trim().toUpperCase()
        ));
    }

    private boolean hasAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
