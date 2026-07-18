package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.util.List;
import java.util.UUID;

/** Internal evidence returned only after Spring successfully commits the statement transaction. */
record ExtraordinaryBenefitStatementOperationResult(
        UUID operationId,
        UUID outboxMessageId,
        RuleLabOperationCardinality cardinality,
        RuleLabOperationBarrier barrier,
        int itemCount,
        int aggregateVisibleItemCount,
        List<ExtraordinaryBenefitRequestResponse> items) {

    ExtraordinaryBenefitStatementOperationResult {
        operationId = java.util.Objects.requireNonNull(operationId, "operationId is required");
        outboxMessageId = java.util.Objects.requireNonNull(outboxMessageId, "outboxMessageId is required");
        items = List.copyOf(items);
    }
}
