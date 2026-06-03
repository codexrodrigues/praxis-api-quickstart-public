package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "veiculoNome", "missaoTitulo", "pilotoNome"})
@Schema(name = "CreateVeiculoMissaoUsoDTO", description = "Corpo de criacao no dominio Ativos; campos do POST (PK gerada no servidor). OpenAPI 3.1 (demo).")
public class CreateVeiculoMissaoUsoDTO extends VeiculoMissaoUsoDTO {
}

