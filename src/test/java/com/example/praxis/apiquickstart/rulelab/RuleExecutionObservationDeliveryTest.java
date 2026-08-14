package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.service.DomainRuleExecutionObservationService;
import org.praxisplatform.rules.contract.RuleDecision;

class RuleExecutionObservationDeliveryTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void publisherDoesNotPropagateOperationalDatabaseFailure() {
        var outbox = mock(RuleExecutionObservationOutbox.class);
        var meters = new SimpleMeterRegistry();
        doThrow(new IllegalStateException("database unavailable")).when(outbox).append(any());
        var publisher = new RuleExecutionObservationPublisher(
                outbox, new ExtraordinaryGrantRuleRuntimeTelemetry(meters));
        publisher.publish("desenv", "local", "snapshot-1", "A".repeat(64),
                1, RuleDecision.ALLOW, 25, NOW);

        assertThat(meters.get("praxis.rule.runtime.execution.observation.outbox")
                .tag("result", "unavailable").counter().count()).isEqualTo(1);
    }

    @Test
    void dispatcherUsesServerOwnedScopeAndAcknowledgesCanonicalIngestion() {
        var outbox = mock(RuleExecutionObservationOutbox.class);
        var service = mock(DomainRuleExecutionObservationService.class);
        UUID id = UUID.randomUUID();
        UUID lease = UUID.randomUUID();
        when(outbox.claim(any(), any())).thenReturn(Optional.of(new RuleExecutionObservationDelivery(
                id, "desenv", "local", "snapshot-1", "A".repeat(64), 7,
                "DENY", 125, NOW, lease, 1)));
        var dispatcher = new RuleExecutionObservationDispatcher(
                outbox, service, Clock.fixed(NOW, ZoneOffset.UTC),
                "service:quickstart", 5, Duration.ofSeconds(30), Duration.ofSeconds(1));

        assertThat(dispatcher.dispatchNext()).isTrue();

        verify(service).ingest(any(), any());
        verify(outbox).delivered(id, lease, NOW);
    }

    @Test
    void dispatcherSchedulesRetryWithoutLosingTheLeaseIdentity() {
        var outbox = mock(RuleExecutionObservationOutbox.class);
        var service = mock(DomainRuleExecutionObservationService.class);
        UUID id = UUID.randomUUID();
        UUID lease = UUID.randomUUID();
        when(outbox.claim(any(), any())).thenReturn(Optional.of(new RuleExecutionObservationDelivery(
                id, "desenv", "local", "snapshot-1", "A".repeat(64), 7,
                "ALLOW", 125, NOW, lease, 2)));
        doThrow(new IllegalStateException("config unavailable")).when(service).ingest(any(), any());
        var dispatcher = new RuleExecutionObservationDispatcher(
                outbox, service, Clock.fixed(NOW, ZoneOffset.UTC),
                "service:quickstart", 5, Duration.ofSeconds(30), Duration.ofSeconds(1));

        assertThat(dispatcher.dispatchNext()).isTrue();

        verify(outbox).failed(id, lease, NOW, 5, Duration.ofSeconds(2), "IllegalStateException");
    }
}
