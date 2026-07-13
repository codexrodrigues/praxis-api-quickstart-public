package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import org.praxisplatform.rules.contract.RuleEvaluationResult;

/** Resultado do engine associado a identidade exata do snapshot selecionado atomicamente. */
public record ExtraordinaryGrantRuleEvaluation(
        RuleEvaluationResult result,
        String snapshotKey,
        String snapshotContentHash,
        long activationRevision) {

    public ExtraordinaryGrantRuleEvaluation {
        result = Objects.requireNonNull(result, "result is required");
        snapshotKey = requireText(snapshotKey, "snapshotKey");
        snapshotContentHash = requireText(snapshotContentHash, "snapshotContentHash");
        if (activationRevision < 1) {
            throw new IllegalArgumentException("activationRevision must be positive");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
