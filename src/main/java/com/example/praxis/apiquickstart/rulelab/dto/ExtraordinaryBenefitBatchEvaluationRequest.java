package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(
        name = "ExtraordinaryBenefitBatchEvaluationRequest",
        description = "Lote limitado de avaliacoes independentes; cada item possui sua propria decisao e transacao de persistencia.")
public record ExtraordinaryBenefitBatchEvaluationRequest(
        @NotEmpty
        @Size(max = 50)
        @Schema(description = "Solicitacoes processadas na ordem recebida, limitadas a cinquenta para manter custo e resposta previsiveis.")
        List<@Valid ExtraordinaryBenefitAuthoritativeEvaluationRequest> requests) {
    public ExtraordinaryBenefitBatchEvaluationRequest {
        requests = requests == null ? List.of() : List.copyOf(requests);
    }
}
