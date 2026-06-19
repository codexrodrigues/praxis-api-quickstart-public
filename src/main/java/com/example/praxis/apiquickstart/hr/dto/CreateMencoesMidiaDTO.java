package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome"})
@Schema(name = "CreateMencoesMidiaDTO", description = "Comando para registrar uma evidencia de midia associada a colaborador ou persona publica, com veiculo, sentimento, URL e data de publicacao para reputacao.")
public class CreateMencoesMidiaDTO extends MencoesMidiaDTO {
}
