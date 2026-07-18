package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Canonical business inputs covered by one statement idempotency key. */
record ExtraordinaryBenefitStatementCommandFingerprint(
        List<ExtraordinaryBenefitEvaluationRequest> items,
        List<String> actorPermissions,
        RuleLabOperationCardinality cardinality) {

    ExtraordinaryBenefitStatementCommandFingerprint {
        items = List.copyOf(Objects.requireNonNull(items, "items are required"));
        actorPermissions = List.copyOf(new TreeSet<>(
                Objects.requireNonNullElse(actorPermissions, List.of())));
        cardinality = Objects.requireNonNull(cardinality, "cardinality is required");
    }

    static ExtraordinaryBenefitStatementCommandFingerprint of(
            List<ExtraordinaryBenefitEvaluationRequest> items,
            Set<String> actorPermissions) {
        return new ExtraordinaryBenefitStatementCommandFingerprint(
                items,
                actorPermissions == null ? List.of() : List.copyOf(actorPermissions),
                RuleLabOperationCardinality.STATEMENT_ATOMIC);
    }
}
