package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "proprietarioNome"})
@Schema(name = "CreateEquipamentoDTO", description = "Corpo de criacao no dominio Ativos; campos do POST (PK gerada no servidor). OpenAPI 3.1 (demo).")
public class CreateEquipamentoDTO extends EquipamentoDTO {
}

