package com.example.praxis.apiquickstart.rulelab.dto;

import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitLifecycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        name = "ExtraordinaryBenefitTransitionResponse",
        description = "Resultado auditavel de uma action item-level, incluindo versao posterior e eventual efeito exatamente uma vez.")
public record ExtraordinaryBenefitTransitionResponse(
        @Schema(description = "Solicitacao alterada pela action.") Long resourceId,
        @Schema(description = "Estado anterior validado dentro da transacao.") ExtraordinaryBenefitLifecycleStatus previousStatus,
        @Schema(description = "Novo estado confirmado depois da transacao.") ExtraordinaryBenefitLifecycleStatus resultingStatus,
        @Schema(description = "Versao JPA posterior usada para o proximo If-Match.") Long resourceVersion,
        @Schema(description = "Identificador da transicao append-only na trilha de auditoria.") UUID transitionId,
        @Schema(description = "Identificador do efeito host-side; preenchido apenas por apply.") UUID effectExecutionId) {
}
