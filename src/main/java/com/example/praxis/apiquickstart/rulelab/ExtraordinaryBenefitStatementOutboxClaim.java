package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.UUID;

/** Internal lease ownership kept outside the event sink contract. */
record ExtraordinaryBenefitStatementOutboxClaim(
        ExtraordinaryBenefitStatementOutboxDelivery delivery,
        UUID leaseToken) {

    ExtraordinaryBenefitStatementOutboxClaim {
        Objects.requireNonNull(delivery, "delivery is required");
        Objects.requireNonNull(leaseToken, "leaseToken is required");
    }
}
