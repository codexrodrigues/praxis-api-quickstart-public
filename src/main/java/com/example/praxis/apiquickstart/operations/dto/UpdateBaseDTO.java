package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateBaseDTO", description = "Comando para substituir os dados editaveis de uma base operacional, preservando sua identidade tecnica e atualizando o cadastro usado por acesso, equipes e missoes.")
public class UpdateBaseDTO extends CreateBaseDTO {
}

