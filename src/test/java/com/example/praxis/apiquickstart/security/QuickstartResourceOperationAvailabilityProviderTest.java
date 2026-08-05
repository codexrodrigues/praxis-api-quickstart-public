package com.example.praxis.apiquickstart.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickstartResourceOperationAvailabilityProviderTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPublishDemoAnalyticsOperationsAsAvailableWhenReadOpenIsEnabled() {
        var provider = new QuickstartResourceOperationAvailabilityProvider(true);

        var decision = provider.evaluate(analyticsContext("statsGroupBy"));

        assertTrue(decision.allowed());
    }

    @Test
    void shouldKeepCorporateAnalyticsUnavailableWithoutAuthority() {
        var provider = new QuickstartResourceOperationAvailabilityProvider(false);

        var decision = provider.evaluate(analyticsContext("statsGroupBy"));

        assertFalse(decision.allowed());
    }

    @Test
    void shouldRequireAuthenticationForMutatingOperationsAcrossQuickstartResources() {
        var provider = new QuickstartResourceOperationAvailabilityProvider(true);
        var context = new ResourceOperationAvailabilityContext(
                "human-resources.funcionarios",
                "/api/human-resources/funcionarios",
                "create",
                "COLLECTION",
                null,
                null,
                Map.of("preferredMethod", "POST"));

        assertFalse(provider.evaluate(context).allowed());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, java.util.List.of()));
        assertTrue(provider.evaluate(context).allowed());
    }

    private ResourceOperationAvailabilityContext analyticsContext(String operationId) {
        return ResourceOperationAvailabilityContext.collection(
                "human-resources.vw-analytics-folha-pagamento",
                "/api/human-resources/vw-analytics-folha-pagamento",
                operationId
        );
    }
}
