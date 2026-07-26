package com.example.praxis.apiquickstart.hr.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HrAnalyticsResourceOperationAvailabilityProviderTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPublishDemoAnalyticsOperationsAsAvailableWhenReadOpenIsEnabled() {
        HrAnalyticsResourceOperationAvailabilityProvider provider =
                new HrAnalyticsResourceOperationAvailabilityProvider(true);

        var decision = provider.evaluate(context("statsGroupBy"));

        assertTrue(decision.allowed());
    }

    @Test
    void shouldKeepCorporateAnalyticsUnavailableWithoutAuthority() {
        HrAnalyticsResourceOperationAvailabilityProvider provider =
                new HrAnalyticsResourceOperationAvailabilityProvider(false);

        var decision = provider.evaluate(context("statsGroupBy"));

        assertFalse(decision.allowed());
    }

    private ResourceOperationAvailabilityContext context(String operationId) {
        return ResourceOperationAvailabilityContext.collection(
                "human-resources.vw-analytics-folha-pagamento",
                "/api/human-resources/vw-analytics-folha-pagamento",
                operationId
        );
    }
}
