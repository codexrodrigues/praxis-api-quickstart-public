package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ExtraordinaryBenefitStatementOutboxDispatcherTest {
    @Test
    void acknowledgementPersistenceFailureNeverSchedulesTransportRetry() throws Exception {
        var leaseService = mock(ExtraordinaryBenefitStatementOutboxLeaseService.class);
        var sink = mock(ExtraordinaryBenefitStatementEventSink.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ExtraordinaryBenefitStatementEventSink> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sink);
        var messageId = UUID.randomUUID();
        var leaseToken = UUID.randomUUID();
        var delivery = new ExtraordinaryBenefitStatementOutboxDelivery(
                messageId, UUID.randomUUID(), "event.v1", "tenant", "test", "correlation",
                new ObjectMapper().createObjectNode(), 1);
        when(leaseService.claimNext(any(), any()))
                .thenReturn(Optional.of(new ExtraordinaryBenefitStatementOutboxClaim(delivery, leaseToken)));
        doThrow(new IllegalStateException("lease lost"))
                .when(leaseService).markDelivered(messageId, leaseToken, Instant.parse("2026-07-15T12:00:00Z"));
        var registry = new SimpleMeterRegistry();
        var dispatcher = new ExtraordinaryBenefitStatementOutboxDispatcher(
                leaseService, provider,
                Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC),
                new ExtraordinaryBenefitStatementOutboxTelemetry(registry), 3, 1000, 100);

        var result = dispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.ACKNOWLEDGEMENT_UNCERTAIN, result.outcome());
        verify(sink).deliver(delivery);
        verify(leaseService, never()).markFailed(any(), any(), any(), any(Integer.class), any(),
                any(Boolean.class), any(), any());
        registry.close();
    }
}
