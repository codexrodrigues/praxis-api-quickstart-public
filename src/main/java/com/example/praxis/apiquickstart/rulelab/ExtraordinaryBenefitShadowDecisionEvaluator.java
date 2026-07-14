package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

/** Boundary testavel para os dois avaliadores isolados do dual-run. */
@FunctionalInterface
interface ExtraordinaryBenefitShadowDecisionEvaluator {
    ExtraordinaryBenefitShadowDecision evaluate(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions,
            Instant nowUtc,
            ZoneId userTimeZone);
}
