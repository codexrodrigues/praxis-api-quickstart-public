package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ExtraordinaryBenefitBatchItemResult",
        description = "Resultado individual do lote, preservando decisao de negocio ou conflito tecnico sem ocultar sucessos parciais.")
public record ExtraordinaryBenefitBatchItemResult(
        @Schema(description = "Referencia externa do item exatamente como recebida.") String requestReference,
        @Schema(description = "Decisao do engine quando a avaliacao foi concluida; ausente para conflito de persistencia.") ExtraordinaryBenefitEvaluationOutcome outcome,
        @Schema(description = "Indica se este item criou um recurso persistido.") boolean persisted,
        @Schema(description = "Identificador do recurso criado, quando persisted=true.") Long resourceId,
        @Schema(description = "Codigo estavel do resultado operacional do item.") String code,
        @Schema(description = "Explicacao segura para tratamento individual pelo operador.") String message) {
}
