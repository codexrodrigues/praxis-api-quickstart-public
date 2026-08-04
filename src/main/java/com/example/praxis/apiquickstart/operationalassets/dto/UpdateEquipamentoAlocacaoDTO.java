package com.example.praxis.apiquickstart.operationalassets.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateEquipamentoAlocacaoDTO",
        description = "Comando para revisar uma custódia de equipamento, incluindo encerramento, troca de responsável ou alteração da situação da alocação.")
public class UpdateEquipamentoAlocacaoDTO extends CreateEquipamentoAlocacaoDTO {
}
