package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados legitimamente comandados pelo caller; estados funcionais e limites ficam fora deste contrato. */
record ExtraordinaryBenefitAuthoritativeEvaluationCommand(
        String requestReference,
        ExtraordinaryBenefitReason reasonCode,
        LocalDate eventDate,
        BigDecimal requestedAmount,
        String factReference,
        LocalDate requestedPaymentDate,
        String userTimeZone) {

    ExtraordinaryBenefitAuthoritativeEvaluationCommand {
        if (requestReference == null || requestReference.isBlank()
                || factReference == null || factReference.isBlank()
                || userTimeZone == null || userTimeZone.isBlank()) {
            throw new IllegalArgumentException("requestReference, factReference and userTimeZone are required");
        }
        if (reasonCode == null || eventDate == null || requestedAmount == null || requestedPaymentDate == null) {
            throw new IllegalArgumentException("reasonCode, eventDate, requestedAmount and requestedPaymentDate are required");
        }
    }
}
