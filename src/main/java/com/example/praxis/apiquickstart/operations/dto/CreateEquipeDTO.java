package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "basePrincipalNome"})
@Schema(name = "CreateEquipeDTO", description = "Corpo de criacao no modulo Operacoes; campos do POST. OpenAPI 3.1 (demo).")
public class CreateEquipeDTO extends EquipeDTO {
}

