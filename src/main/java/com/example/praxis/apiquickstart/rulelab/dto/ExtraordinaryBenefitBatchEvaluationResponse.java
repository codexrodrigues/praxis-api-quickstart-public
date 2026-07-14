package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
        name = "ExtraordinaryBenefitBatchEvaluationResponse",
        description = "Fechamento nao atomico do lote com contadores e resultados na ordem original.")
public record ExtraordinaryBenefitBatchEvaluationResponse(
        @Schema(description = "Sempre false: cada item elegivel usa transacao independente para permitir falha parcial segura.") boolean atomic,
        @Schema(description = "Quantidade total de itens recebidos.") int total,
        @Schema(description = "Quantidade de recursos efetivamente persistidos.") int persisted,
        @Schema(description = "Quantidade de itens negados, inconclusivos, nao aplicaveis ou conflitantes.") int notPersisted,
        @Schema(description = "Resultados individuais na mesma ordem do request.") List<ExtraordinaryBenefitBatchItemResult> items) {
    public ExtraordinaryBenefitBatchEvaluationResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
