package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateReputacaoDTO", description = "Comando para revisar scores e frescor de um snapshot reputacional existente, preservando o vinculo ao colaborador e a identidade tecnica do registro.")
public class UpdateReputacaoDTO extends CreateReputacaoDTO {
}
