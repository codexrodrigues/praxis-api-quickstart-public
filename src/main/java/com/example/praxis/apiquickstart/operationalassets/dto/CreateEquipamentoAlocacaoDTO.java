package com.example.praxis.apiquickstart.operationalassets.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateEquipamentoAlocacaoDTO",
        description = "Comando para registrar uma nova custódia de equipamento por colaborador, com início de vigência e situação da responsabilidade operacional.")
public class CreateEquipamentoAlocacaoDTO extends EquipamentoAlocacaoDTO {
}
