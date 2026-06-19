package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateBaseDTO", description = "Comando para cadastrar uma instalacao operacional, definindo nome, tipo, sigilo, georeferencia e planeta usados por logistica, acesso e planejamento de missoes.")
public class CreateBaseDTO extends BaseDTO {
}

