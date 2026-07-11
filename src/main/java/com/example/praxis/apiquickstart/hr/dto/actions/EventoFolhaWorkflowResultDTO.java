package com.example.praxis.apiquickstart.hr.dto.actions;

import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Resultado de uma transição individual de evento de folha, com a evidência auditável e o estado efetivamente persistido.")
public record EventoFolhaWorkflowResultDTO(
        @Schema(description = "Identificador da transição append-only criada para a decisão.") UUID transitionId,
        @Schema(description = "Estado persistido do evento após a decisão de workflow.") StatusEventoFolha status) { }
