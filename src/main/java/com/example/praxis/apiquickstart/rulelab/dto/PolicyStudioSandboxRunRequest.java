package com.example.praxis.apiquickstart.rulelab.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Comando host-owned para avaliar cenarios governados sem executar efeitos operacionais.
 *
 * @param workspaceId workspace canônico do Config
 * @param scenarioIds cenarios selecionados; vazio significa todos os cenarios ativos
 * @param userTimeZone timezone IANA explicito da avaliacao
 * @param idempotencyKey identidade estavel do mesmo comando para retry seguro
 * @param evaluatedAtUtc relogio congelado e reutilizado em todo retry do mesmo comando
 */
public record PolicyStudioSandboxRunRequest(
        UUID workspaceId,
        List<UUID> scenarioIds,
        String userTimeZone,
        String idempotencyKey,
        Instant evaluatedAtUtc) {
}
