package com.example.praxis.apiquickstart.operationalassets.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateEquipamentoDTO",
        description = "Comando para revisar um equipamento existente quando mudam classificacao tatica, resistencia, custodiante ou status de disponibilidade.")
public class UpdateEquipamentoDTO extends CreateEquipamentoDTO {
}

