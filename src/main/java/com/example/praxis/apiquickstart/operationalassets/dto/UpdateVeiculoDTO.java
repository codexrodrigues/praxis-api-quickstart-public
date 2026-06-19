package com.example.praxis.apiquickstart.operationalassets.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateVeiculoDTO",
        description = "Comando para revisar um veiculo existente quando mudam capacidade, responsavel, tipo de plataforma ou disponibilidade para missoes.")
public class UpdateVeiculoDTO extends CreateVeiculoDTO {
}

