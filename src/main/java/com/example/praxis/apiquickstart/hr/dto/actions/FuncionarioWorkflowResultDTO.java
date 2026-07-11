package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Resultado confirmado de uma transição de lifecycle de funcionário.")
public record FuncionarioWorkflowResultDTO(UUID transitionId, boolean ativo) { }
