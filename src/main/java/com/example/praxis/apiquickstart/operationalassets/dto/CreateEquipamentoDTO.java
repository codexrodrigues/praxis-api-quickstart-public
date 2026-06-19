package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "proprietarioNome"})
@Schema(
        name = "CreateEquipamentoDTO",
        description = "Comando para cadastrar um equipamento operacional no inventario, definindo designacao, tipo, resistencia, custodiante inicial e status de disponibilidade.")
public class CreateEquipamentoDTO extends EquipamentoDTO {
}

