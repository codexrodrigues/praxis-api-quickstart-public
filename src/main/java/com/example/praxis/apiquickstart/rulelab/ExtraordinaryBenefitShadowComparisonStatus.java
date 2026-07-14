package com.example.praxis.apiquickstart.rulelab;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado operacional fechado da comparacao shadow; nunca concede autoridade ao candidato. */
@Schema(description = "Classificacao operacional da comparacao entre baseline sintetico e candidato Praxis.")
public enum ExtraordinaryBenefitShadowComparisonStatus {
    MATCH,
    MISMATCH,
    INCONCLUSIVE,
    TECHNICAL_ERROR
}
