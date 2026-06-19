package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateFeriasAfastamentoDTO", description = "Comando para revisar o periodo, tipo ou observacoes de uma ausencia ja registrada, preservando sua identidade e mantendo o impacto em disponibilidade e folha.")
public class UpdateFeriasAfastamentoDTO extends CreateFeriasAfastamentoDTO {
}
