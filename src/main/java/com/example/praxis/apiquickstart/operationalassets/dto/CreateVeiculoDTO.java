package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "proprietarioNome"})
@Schema(
        name = "CreateVeiculoDTO",
        description = "Comando para cadastrar um veiculo de frota operacional, informando designacao, tipo de plataforma, capacidade, responsavel e status inicial.")
public class CreateVeiculoDTO extends VeiculoDTO {
}

