package com.example.praxis.apiquickstart.rulelab;

import java.math.BigDecimal;
import java.util.Objects;
import org.praxisplatform.rules.contract.TypedTransformationProposal;

/** Authorized host materialization paired with the exact engine proposal that produced it. */
record ExtraordinaryBenefitMaterializedTransformation(
        BigDecimal amount,
        TypedTransformationProposal proposal) {

    ExtraordinaryBenefitMaterializedTransformation {
        amount = Objects.requireNonNull(amount, "amount is required");
        proposal = Objects.requireNonNull(proposal, "proposal is required");
    }
}
