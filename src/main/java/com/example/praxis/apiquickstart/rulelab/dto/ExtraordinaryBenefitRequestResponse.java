package com.example.praxis.apiquickstart.rulelab.dto;

import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitEffectStatus;
import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitLifecycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
        name = "ExtraordinaryBenefitRequestResponse",
        description = "Solicitacao elegivel persistida com identidade, lifecycle, evidencia deterministica e versao usada por comandos concorrentes.")
public record ExtraordinaryBenefitRequestResponse(
        @Schema(description = "Identificador interno estavel usado nas rotas item-level e nos vinculos de auditoria.")
        Long id,
        @Schema(description = "Estado confirmado do lifecycle; somente actions explicitas podem avancar essa sequencia.")
        ExtraordinaryBenefitLifecycleStatus lifecycleStatus,
        @Schema(description = "Fatos de negocio originalmente congelados para a avaliacao que criou o recurso.")
        ExtraordinaryBenefitEvaluationRequest facts,
        @Schema(description = "Decisao ALLOW e evidencia exata do snapshot que autorizou a persistencia.")
        ExtraordinaryBenefitEvaluationResponse evaluation,
        @Schema(description = "Estado host-side do efeito planejado; EXECUTED existe apenas depois da action apply.")
        ExtraordinaryBenefitEffectStatus effectStatus,
        @Schema(description = "Versao persistida do agregado usada para produzir e validar o ETag de recurso.")
        Long version,
        @Schema(description = "Instante UTC em que a avaliacao ALLOW foi persistida.")
        Instant evaluatedAtUtc,
        @Schema(description = "Instante UTC em que o pedido foi submetido para aprovacao, quando aplicavel.")
        Instant submittedAtUtc,
        @Schema(description = "Instante UTC em que a aprovacao foi confirmada, quando aplicavel.")
        Instant approvedAtUtc,
        @Schema(description = "Instante UTC em que o efeito foi executado e o pedido tornou-se aplicado.")
        Instant appliedAtUtc) {
}
