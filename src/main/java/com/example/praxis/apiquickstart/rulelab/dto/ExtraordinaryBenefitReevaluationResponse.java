package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe outcome of one idempotent, optimistic re-evaluation command. */
@Schema(
        name = "ExtraordinaryBenefitReevaluationResponse",
        description = "Resultado da reavaliação e indicação explícita de mutação. Outcomes não autorizadores preservam o agregado.")
public record ExtraordinaryBenefitReevaluationResponse(
        @Schema(description = "Identidade estável da solicitação reavaliada.")
        Long id,
        @Schema(description = "Decisão produzida com facts e snapshot vigentes.")
        ExtraordinaryBenefitEvaluationResponse evaluation,
        @Schema(description = "Estado persistido após o comando; permanece igual quando a mutação não é autorizada.")
        ExtraordinaryBenefitRequestResponse resource,
        @Schema(description = "Indica se o agregado foi efetivamente alterado.")
        boolean mutationObserved,
        @Schema(description = "Versão do agregado antes da execução.")
        long previousVersion,
        @Schema(description = "Versão do agregado após a execução.")
        long currentVersion) {
}
