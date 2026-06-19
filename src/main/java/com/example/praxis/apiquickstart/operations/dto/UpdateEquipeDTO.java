package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateEquipeDTO", description = "Comando para substituir os dados editaveis de uma equipe operacional, preservando sua identidade e recalculando leituras como base principal no retorno.")
public class UpdateEquipeDTO extends CreateEquipeDTO {
}

