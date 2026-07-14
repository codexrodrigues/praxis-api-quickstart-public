package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(
        name = "ExtraordinaryBenefitTransitionRequest",
        description = "Justificativa publica de uma transicao do lifecycle; autoridade e identidade do ator sao resolvidas pelo servidor.")
public record ExtraordinaryBenefitTransitionRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "Motivo auditavel para submeter, aprovar ou aplicar o beneficio, sem dados de credencial ou contexto interno.")
        String justification,
        @NotNull
        @Schema(description = "Data de negocio em que a transicao deve produzir efeitos de auditoria e competencia.")
        LocalDate effectiveAt) {
}
