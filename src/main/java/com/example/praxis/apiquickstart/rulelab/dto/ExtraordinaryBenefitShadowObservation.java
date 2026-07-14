package com.example.praxis.apiquickstart.rulelab.dto;

import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitShadowComparisonStatus;
import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitShadowSideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
        name = "ExtraordinaryBenefitShadowObservation",
        description = "Amostra operacional sanitizada do dual-run; nao contem referencia da solicitacao, fatos, valores monetarios, ator, segredo ou payload de snapshot.")
public record ExtraordinaryBenefitShadowObservation(
        @Schema(description = "Identificador aleatorio da observacao, sem derivacao dos fatos de negocio.")
        String observationId,
        @Schema(description = "Instante UTC congelado compartilhado pelos dois avaliadores.")
        Instant observedAtUtc,
        @Schema(description = "Classificacao final da comparacao, sem conceder autoridade ao candidato.")
        ExtraordinaryBenefitShadowComparisonStatus comparisonStatus,
        @Schema(description = "Estado isolado da execucao do baseline sintetico.")
        ExtraordinaryBenefitShadowSideStatus baselineStatus,
        @Schema(description = "Estado isolado da execucao do snapshot Praxis candidato.")
        ExtraordinaryBenefitShadowSideStatus candidateStatus,
        @Schema(description = "Conclusao de negocio do baseline quando sua execucao terminou com sucesso.")
        ExtraordinaryBenefitEvaluationOutcome baselineOutcome,
        @Schema(description = "Conclusao de negocio do candidato quando sua execucao terminou com sucesso.")
        ExtraordinaryBenefitEvaluationOutcome candidateOutcome,
        @Schema(description = "Indica equivalencia da conclusao em cinco estados.")
        boolean outcomeMatch,
        @Schema(description = "Indica equivalencia ordenada dos codigos de motivo, sem publicar os codigos na amostra.")
        boolean reasonCodesMatch,
        @Schema(description = "Indica equivalencia por valor da recomendacao monetaria, sem publicar o valor.")
        boolean recommendedAmountMatch,
        @Schema(description = "Indica equivalencia do tipo e estado da intencao de efeito planejada, sem executar o efeito.")
        boolean plannedEffectMatch,
        @Schema(description = "Snapshot candidato efetivamente observado; nulo quando o candidato falha antes da selecao.")
        String candidateSnapshotKey,
        @Schema(description = "Hash de conteudo do snapshot candidato, seguro para correlacao operacional sem expor o payload.")
        String candidateSnapshotContentHash,
        @Schema(description = "Confirma que a amostra segue a allowlist de campos operacionais e omite dados pessoais e fatos.")
        boolean sanitized,
        @Schema(description = "Sempre falso: o caminho shadow nao cria nem altera solicitacao.")
        boolean persisted,
        @Schema(description = "Sempre falso: o caminho shadow nao executa ledger nem integracao externa.")
        boolean effectExecuted) {
}
