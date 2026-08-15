package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Host action command for an operational Policy Studio Test Run.
 *
 * <p>The caller selects governed scenarios and operation modes only. Business commands, fixture
 * references, persistence details and cleanup remain private to the host adapter.</p>
 */
public record PolicyStudioOperationalRunRequest(
        @NotNull
        @Schema(description = "Workspace canônico do Config que owns cenários e Test Runs.")
        UUID workspaceId,

        @NotEmpty @Size(max = 20)
        @Schema(description = "Cenários governados com operação explícita, sem payload de DML.")
        List<@Valid PolicyStudioOperationalScenarioSelection> scenarios,

        @Size(max = 80)
        @Schema(description = "Fuso IANA congelado para a execução.", example = "America/Sao_Paulo")
        String userTimeZone,

        @NotNull
        @Schema(description = "Instante UTC congelado e reutilizado em retries do mesmo comando.")
        Instant evaluatedAtUtc) {
}
