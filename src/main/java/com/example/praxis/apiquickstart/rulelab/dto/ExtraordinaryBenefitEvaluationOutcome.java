package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado de negocio em cinco estados produzido pela avaliacao deterministica. */
@Schema(description = "Conclusao da avaliacao: autorizar, negar, nao aplicar, permanecer inconclusiva ou indicar falha tecnica controlada.")
public enum ExtraordinaryBenefitEvaluationOutcome {
    ALLOW,
    DENY,
    NOT_APPLICABLE,
    INCONCLUSIVE,
    TECHNICAL_ERROR
}
