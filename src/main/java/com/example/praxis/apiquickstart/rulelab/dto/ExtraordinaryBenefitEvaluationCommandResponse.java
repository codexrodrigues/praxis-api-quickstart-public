package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ExtraordinaryBenefitEvaluationCommandResponse",
        description = "Resultado da action evaluate: sempre devolve a decisao e inclui o recurso apenas quando ALLOW foi persistido.")
public record ExtraordinaryBenefitEvaluationCommandResponse(
        @Schema(description = "Resultado deterministico completo, inclusive para DENY, NOT_APPLICABLE, INCONCLUSIVE e TECHNICAL_ERROR.")
        ExtraordinaryBenefitEvaluationResponse evaluation,
        @Schema(description = "Recurso persistido para ALLOW; ausente para qualquer resultado que nao autorize persistencia.")
        ExtraordinaryBenefitRequestResponse resource) {
}
