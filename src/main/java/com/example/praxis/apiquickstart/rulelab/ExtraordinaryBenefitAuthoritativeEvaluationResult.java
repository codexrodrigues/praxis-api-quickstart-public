package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;

/** Resultado interno que preserva a decisao e a provenance exata dos facts usados. */
record ExtraordinaryBenefitAuthoritativeEvaluationResult(
        ExtraordinaryBenefitEvaluationRequest resolvedRequest,
        ExtraordinaryBenefitEvaluatedDecision decision,
        RuleFactProvenance factProvenance) {

    ExtraordinaryBenefitAuthoritativeEvaluationResult {
        if (resolvedRequest == null || decision == null || factProvenance == null) {
            throw new IllegalArgumentException("resolvedRequest, decision and factProvenance are required");
        }
    }
}
