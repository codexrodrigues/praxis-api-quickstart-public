package com.example.praxis.apiquickstart.rulelab;

import java.util.Optional;
import java.util.UUID;

/** Queries the external consumer after an ambiguous delivery result. */
public interface ExtraordinaryBenefitStatementDeliveryProbe {
    Optional<ExtraordinaryBenefitStatementExternalAcknowledgement> findAcknowledgement(UUID messageId)
            throws Exception;
}
