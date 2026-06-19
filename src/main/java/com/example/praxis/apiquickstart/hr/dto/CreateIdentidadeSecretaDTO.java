package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome"})
@Schema(name = "CreateIdentidadeSecretaDTO", description = "Comando para criar a persona publica de um colaborador, vinculando funcionario, codinome, universo e politica de exposicao sem aceitar campos civis resolvidos.")
public class CreateIdentidadeSecretaDTO extends IdentidadeSecretaDTO {
}
