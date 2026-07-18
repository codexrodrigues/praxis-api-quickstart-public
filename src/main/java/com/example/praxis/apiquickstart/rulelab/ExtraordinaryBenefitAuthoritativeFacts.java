package com.example.praxis.apiquickstart.rulelab;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Facts de elegibilidade que pertencem ao host e nunca ao payload de simulacao do caller. */
record ExtraordinaryBenefitAuthoritativeFacts(
        String workerStatus,
        boolean duplicateGrant,
        boolean programActive,
        BigDecimal programMaximumAmount,
        Boolean customerAdditionalEligible,
        List<LocalDate> allowedPaymentDates,
        BigDecimal availableBudgetAmount) {

    ExtraordinaryBenefitAuthoritativeFacts {
        if (workerStatus == null || workerStatus.isBlank()) {
            throw new IllegalArgumentException("workerStatus is required");
        }
        if (programMaximumAmount == null || availableBudgetAmount == null) {
            throw new IllegalArgumentException("programMaximumAmount and availableBudgetAmount are required");
        }
        allowedPaymentDates = allowedPaymentDates == null ? List.of() : List.copyOf(allowedPaymentDates);
    }
}
