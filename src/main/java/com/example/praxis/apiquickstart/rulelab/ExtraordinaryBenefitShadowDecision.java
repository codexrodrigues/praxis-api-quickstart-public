package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import java.math.BigDecimal;
import java.util.List;

/** Projecao minima comum usada apenas para comparar duas decisoes independentes. */
record ExtraordinaryBenefitShadowDecision(
        ExtraordinaryBenefitEvaluationOutcome outcome,
        List<String> reasonCodes,
        BigDecimal recommendedAmount,
        String currency,
        String plannedEffectIntent,
        String plannedEffectStatus,
        String snapshotKey,
        String snapshotContentHash) {

    ExtraordinaryBenefitShadowDecision {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }

    static ExtraordinaryBenefitShadowDecision candidate(ExtraordinaryBenefitEvaluationResponse response) {
        return new ExtraordinaryBenefitShadowDecision(
                response.outcome(), response.reasonCodes(), response.recommendedAmount(), response.currency(),
                response.plannedEffectIntent(), response.plannedEffectStatus(),
                response.snapshotKey(), response.snapshotContentHash());
    }

    static ExtraordinaryBenefitShadowDecision baseline(
            ExtraordinaryBenefitEvaluationOutcome outcome,
            List<String> reasonCodes,
            BigDecimal recommendedAmount,
            String currency,
            String plannedEffectIntent,
            String plannedEffectStatus) {
        return new ExtraordinaryBenefitShadowDecision(
                outcome, reasonCodes, recommendedAmount, currency,
                plannedEffectIntent, plannedEffectStatus, null, null);
    }
}
