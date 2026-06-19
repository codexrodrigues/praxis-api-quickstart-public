package com.example.praxis.apiquickstart.operationalassets.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateVeiculoMissaoUsoDTO",
        description = "Comando para revisar uma sortie de veiculo, como piloto, horarios de partida ou chegada e observacoes operacionais.")
public class UpdateVeiculoMissaoUsoDTO extends CreateVeiculoMissaoUsoDTO {
}

