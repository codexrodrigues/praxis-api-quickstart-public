package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Appends the external delivery envelope inside the caller's operational transaction. */
@Service
final class ExtraordinaryBenefitStatementOutboxWriter {
    static final String EVENT_TYPE = "human-resources.extraordinary-benefit.statement-evaluated.v1";

    private final ExtraordinaryBenefitStatementOutboxRepository repository;
    private final ObjectMapper objectMapper;

    ExtraordinaryBenefitStatementOutboxWriter(
            ExtraordinaryBenefitStatementOutboxRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    UUID append(
            RuleLabOperationContext context,
            List<ExtraordinaryBenefitRequestResponse> persistedItems) {
        UUID messageId = UUID.randomUUID();
        var payload = new ExtraordinaryBenefitStatementOutboxPayload(
                context.operationId(),
                persistedItems.stream().map(ExtraordinaryBenefitRequestResponse::id).toList(),
                context.itemCount(),
                context.snapshotKey(),
                context.snapshotContentHash(),
                context.snapshotActivationRevision(),
                context.nowUtc());
        repository.saveAndFlush(new ExtraordinaryBenefitStatementOutboxMessage(
                messageId,
                context.operationId(),
                EVENT_TYPE,
                context.tenantId(),
                context.environment(),
                context.correlationId(),
                objectMapper.valueToTree(payload),
                context.nowUtc()));
        return messageId;
    }
}
