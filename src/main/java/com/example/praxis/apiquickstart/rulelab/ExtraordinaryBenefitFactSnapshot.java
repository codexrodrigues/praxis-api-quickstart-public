package com.example.praxis.apiquickstart.rulelab;

/** Snapshot tipado carregado em uma unica transacao somente leitura. */
record ExtraordinaryBenefitFactSnapshot(
        ExtraordinaryBenefitAuthoritativeFacts facts,
        RuleFactProvenance provenance) {

    ExtraordinaryBenefitFactSnapshot {
        if (facts == null || provenance == null) {
            throw new IllegalArgumentException("facts and provenance are required");
        }
    }
}
