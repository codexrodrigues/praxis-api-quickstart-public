package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class ReactiveDeterminationLkgHealthIndicatorTest {
    @Test
    void exposesOnlyRedactedLowCardinalityReadiness() {
        AppliedReactiveDeterminationResolver resolver = mock(AppliedReactiveDeterminationResolver.class);
        when(resolver.lkgStatus()).thenReturn(new ReactiveDeterminationLkgStatus(
                true, "lkg", 2, Instant.parse("2026-08-13T18:00:00Z"), null));

        var health = new ReactiveDeterminationLkgHealthIndicator(resolver).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("mode", "lkg").containsEntry("cachedScopeCount", 2);
        assertThat(health.getDetails().keySet()).noneMatch(key ->
                key.toLowerCase().contains("tenant") || key.toLowerCase().contains("hash")
                        || key.toLowerCase().contains("etag") || key.toLowerCase().contains("snapshot"));
    }
}
