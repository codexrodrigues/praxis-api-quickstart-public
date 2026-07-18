package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import java.util.Objects;

/** Internal host result that keeps redacted transformation evidence out of the public HTTP DTO. */
record ExtraordinaryBenefitEvaluatedDecision(
        ExtraordinaryBenefitEvaluationResponse response,
        ExtraordinaryBenefitTransformationAuditEvidence transformationEvidence) {

    ExtraordinaryBenefitEvaluatedDecision {
        response = Objects.requireNonNull(response, "response is required");
    }
}
