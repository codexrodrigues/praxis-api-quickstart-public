package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Selects a governed scenario and the host operation that must prove it. */
public record PolicyStudioOperationalScenarioSelection(
        @NotNull
        @Schema(description = "Identificador do cenário governado no workspace do Config.")
        UUID scenarioId,

        @NotBlank @Size(max = 16)
        @Schema(description = "Operação operacional explícita; o host aceita CREATE ou UPDATE.",
                allowableValues = {"CREATE", "UPDATE"})
        String operationMode) {
}
