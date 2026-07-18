package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

/** Verifies the aggregate local-flush barrier before a statement transaction may commit. */
@Component
class ExtraordinaryBenefitStatementBarrier {
    private final ExtraordinaryBenefitRequestRepository repository;

    ExtraordinaryBenefitStatementBarrier(ExtraordinaryBenefitRequestRepository repository) {
        this.repository = repository;
    }

    int verify(
            RuleLabOperationContext context,
            List<ExtraordinaryBenefitEvaluationResponse> evaluations,
            List<ExtraordinaryBenefitRequestResponse> persisted) {
        if (evaluations.size() != context.itemCount() || persisted.size() != context.itemCount()) {
            throw new IllegalStateException("Statement item count changed across evaluation and persistence");
        }
        if (evaluations.stream().anyMatch(evaluation -> evaluation.outcome() != ExtraordinaryBenefitEvaluationOutcome.ALLOW)) {
            throw new IllegalStateException("STATEMENT_ATOMIC accepts only fully allowed evaluation sets");
        }
        if (evaluations.stream().anyMatch(evaluation ->
                !context.nowUtc().equals(evaluation.evaluatedAtUtc())
                        || !context.snapshotKey().equals(evaluation.snapshotKey())
                        || !context.snapshotContentHash().equals(evaluation.snapshotContentHash())
                        || context.snapshotActivationRevision() != evaluation.snapshotActivationRevision())) {
            throw new IllegalStateException("Statement items did not use one frozen snapshot and instant");
        }

        var ids = persisted.stream().map(ExtraordinaryBenefitRequestResponse::id).toList();
        int locallyVisible = (int) StreamSupport.stream(repository.findAllById(ids).spliterator(), false).count();
        if (locallyVisible != context.itemCount()) {
            throw new IllegalStateException("Statement items are not visible at the LOCAL_FLUSHED barrier");
        }
        return locallyVisible;
    }
}
