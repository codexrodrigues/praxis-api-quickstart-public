package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;

/**
 * Safe transport failure exposed by an external statement adapter to the outbox dispatcher.
 *
 * <p>The code is persisted for operations without retaining response bodies or credentials. A
 * permanent failure moves the message directly to dead-letter; a transient failure remains
 * eligible for the dispatcher's bounded retry policy.</p>
 */
final class ExtraordinaryBenefitStatementDeliveryFailure extends Exception {
    private final String failureCode;
    private final boolean permanent;

    private ExtraordinaryBenefitStatementDeliveryFailure(String failureCode, boolean permanent) {
        super(Objects.requireNonNull(failureCode, "failureCode is required"));
        this.failureCode = failureCode;
        this.permanent = permanent;
    }

    static ExtraordinaryBenefitStatementDeliveryFailure transientFailure(String failureCode) {
        return new ExtraordinaryBenefitStatementDeliveryFailure(failureCode, false);
    }

    static ExtraordinaryBenefitStatementDeliveryFailure permanentFailure(String failureCode) {
        return new ExtraordinaryBenefitStatementDeliveryFailure(failureCode, true);
    }

    String failureCode() {
        return failureCode;
    }

    boolean permanent() {
        return permanent;
    }
}
