package com.example.praxis.apiquickstart.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"filter", "cursor", "locate", "options", "optionSources", "export",
            "statsGroupBy", "statsTimeSeries", "statsDistribution", "statsComparison"})
    void shouldPublishPostQueriesOnlyWhenPublicReadsAreEnabled(String operationId) {
        var context = new ResourceOperationAvailabilityContext(
                "operations.missoes", "/api/operations/missoes", operationId,
                "COLLECTION", null, null, Map.of("preferredMethod", "POST"));
        assertTrue(new QuickstartResourceOperationAvailabilityProvider(true).evaluate(context).allowed());
        assertFalse(new QuickstartResourceOperationAvailabilityProvider(false).evaluate(context).allowed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "update", "delete", "unknown-command"})
    void shouldNotGrantCommandsInThePublicDemo(String operationId) {
        var context = new ResourceOperationAvailabilityContext(
                "operations.missoes", "/api/operations/missoes", operationId,
                "COLLECTION", null, null, Map.of("preferredMethod", "POST"));
        assertFalse(new QuickstartResourceOperationAvailabilityProvider(true).evaluate(context).allowed());
    }

    private ResourceOperationAvailabilityContext analyticsContext(String operationId) {
        return new ResourceOperationAvailabilityContext(
                "human-resources.vw-analytics-folha-pagamento",
                "/api/human-resources/vw-analytics-folha-pagamento",
                operationId, "COLLECTION", null, null, Map.of("preferredMethod", "POST")
        );
    }
}
