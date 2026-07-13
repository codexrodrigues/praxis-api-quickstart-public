package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(
        name = "ExtraordinaryBenefitEvaluationResponse",
        description = "Resultado explicavel e auditavel da avaliacao deterministica; nao representa concessao persistida nem efeito executado.")
public record ExtraordinaryBenefitEvaluationResponse(
        @Schema(description = "Referencia recebida do processo solicitante para correlacao sem criar um registro persistido.")
        String requestReference,
        @Schema(description = "Conclusao de negocio em cinco estados produzida pelo RuleSet governado.")
        ExtraordinaryBenefitEvaluationOutcome outcome,
        @Schema(description = "Mensagem orientada ao operador que resume a consequencia pratica da conclusao.")
        String businessMessage,
        @Schema(description = "Codigos estaveis que explicam negacao, nao aplicabilidade, inconclusao ou falha controlada.")
        List<String> reasonCodes,
        @Schema(description = "Instante UTC congelado usado por toda a avaliacao.")
        Instant evaluatedAtUtc,
        @Schema(description = "Chave imutavel do snapshot governado efetivamente selecionado pelo host.")
        String snapshotKey,
        @Schema(description = "Hash SHA-256 do conteudo do snapshot, usado para provar exatamente qual politica foi avaliada.")
        String snapshotContentHash,
        @Schema(description = "Revisao monotona da ativacao no control plane observada pelo host.")
        long snapshotActivationRevision,
        @Schema(description = "Identidade estavel do conjunto de regras avaliado.")
        String ruleSetKey,
        @Schema(description = "Versao imutavel do conjunto de regras avaliado.")
        int ruleSetVersion,
        @Schema(description = "Hash dos fatos canonicos congelados, permitindo comparar repeticoes sem expor o payload completo.")
        String factsDigest,
        @Schema(description = "Hash do plano compilado que prova ordem, dependencias e bindings efetivamente executados.")
        String planDigest,
        @Schema(description = "Valor calculado recomendado quando as etapas anteriores permitem executar o calculo.")
        BigDecimal recommendedAmount,
        @Schema(description = "Moeda do valor calculado; o piloto atual opera exclusivamente em BRL.")
        String currency,
        @Schema(description = "Tipo do efeito que uma etapa posterior poderia executar apos persistencia e aprovacao.")
        String plannedEffectIntent,
        @Schema(description = "Estado do efeito puro produzido pelo engine; no QL-04 deve permanecer PLANNED_NOT_EXECUTED.")
        String plannedEffectStatus,
        @Schema(description = "Confirma que esta action nao criou nem alterou uma solicitacao persistida.")
        boolean persisted,
        @Schema(description = "Confirma que nenhum efeito externo foi executado durante a avaliacao.")
        boolean effectExecuted) {

    public ExtraordinaryBenefitEvaluationResponse {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
