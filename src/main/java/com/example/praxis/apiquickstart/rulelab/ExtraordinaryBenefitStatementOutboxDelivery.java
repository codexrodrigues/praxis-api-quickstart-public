package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.UUID;

/** Immutable delivery envelope; retries preserve messageId and operationId. */
public record ExtraordinaryBenefitStatementOutboxDelivery(
        UUID messageId,
        UUID operationId,
        String eventType,
        String tenantId,
        String environment,
        String correlationId,
        JsonNode payload,
        int attempt) {

    public ExtraordinaryBenefitStatementOutboxDelivery {
        Objects.requireNonNull(messageId, "messageId is required");
        Objects.requireNonNull(operationId, "operationId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(environment, "environment is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        payload = Objects.requireNonNull(payload, "payload is required").deepCopy();
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
    }

    @Override
    public JsonNode payload() {
        return payload.deepCopy();
    }
}
