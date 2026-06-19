package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "veiculoNome", "missaoTitulo", "pilotoNome"})
@Schema(
        name = "CreateVeiculoMissaoUsoDTO",
        description = "Comando para registrar o uso de um veiculo em uma missao, vinculando frota, missao, piloto, partida e observacoes da sortie.")
public class CreateVeiculoMissaoUsoDTO extends VeiculoMissaoUsoDTO {
}

