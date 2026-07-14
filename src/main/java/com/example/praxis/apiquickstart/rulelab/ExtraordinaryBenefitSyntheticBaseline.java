package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Baseline sintetico deliberadamente independente do RuleSet e do resultado Praxis.
 *
 * <p>Ele implementa a especificacao congelada do laboratorio diretamente sobre o
 * comando validado. Nao le snapshot, banco, resposta do candidato ou payload
 * persistido, portanto uma divergencia continua detectavel.</p>
 */
final class ExtraordinaryBenefitSyntheticBaseline implements ExtraordinaryBenefitShadowDecisionEvaluator {
    private static final String EFFECT_INTENT = "REGISTER_EXTRAORDINARY_GRANT";
    private static final String EFFECT_STATUS = "PLANNED_NOT_EXECUTED";

    @Override
    public ExtraordinaryBenefitShadowDecision evaluate(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions,
            Instant nowUtc,
            ZoneId userTimeZone) {
        if (actorPermissions == null || !actorPermissions.contains("benefit:request")) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "REQUEST_NOT_AUTHORIZED");
        }
        if (!"ACTIVE".equals(request.workerStatus())) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "WORKER_NOT_ELIGIBLE");
        }
        if (Boolean.TRUE.equals(request.duplicateGrant())) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "DUPLICATE_GRANT");
        }
        if (!Boolean.TRUE.equals(request.programActive())) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.NOT_APPLICABLE, "PROGRAM_NOT_APPLICABLE");
        }
        if (request.customerAdditionalEligible() == null) {
            return ExtraordinaryBenefitShadowDecision.baseline(
                    ExtraordinaryBenefitEvaluationOutcome.INCONCLUSIVE,
                    List.of("FACT_REQUIRED_MISSING", "PRIOR_DECISION_INCONCLUSIVE"),
                    null, null, null, null);
        }
        if (!request.customerAdditionalEligible()) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "CUSTOMER_POLICY_RESTRICTED");
        }
        if (!request.allowedPaymentDates().contains(request.requestedPaymentDate())) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "PAYMENT_DATE_NOT_ALLOWED");
        }
        if (request.requestedAmount().compareTo(request.programMaximumAmount()) > 0) {
            return decision(ExtraordinaryBenefitEvaluationOutcome.DENY, "REQUESTED_AMOUNT_EXCEEDS_PROGRAM_LIMIT");
        }

        BigDecimal recommended = request.requestedAmount().setScale(2, RoundingMode.HALF_EVEN);
        if (request.availableBudgetAmount().compareTo(request.requestedAmount()) < 0) {
            return ExtraordinaryBenefitShadowDecision.baseline(
                    ExtraordinaryBenefitEvaluationOutcome.DENY,
                    List.of("BUDGET_INSUFFICIENT"), recommended, "BRL", null, null);
        }
        return ExtraordinaryBenefitShadowDecision.baseline(
                ExtraordinaryBenefitEvaluationOutcome.ALLOW,
                List.of(), recommended, "BRL", EFFECT_INTENT, EFFECT_STATUS);
    }

    private ExtraordinaryBenefitShadowDecision decision(
            ExtraordinaryBenefitEvaluationOutcome outcome,
            String reasonCode) {
        return ExtraordinaryBenefitShadowDecision.baseline(
                outcome, List.of(reasonCode), null, null, null, null);
    }
}
