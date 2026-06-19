package com.example.praxis.apiquickstart.operationalassets.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateEquipamentoAlocacaoDTO",
        description = "Comando para revisar uma custodia de equipamento, incluindo encerramento, troca de responsavel ou alteracao do status da alocacao.")
public class UpdateEquipamentoAlocacaoDTO extends CreateEquipamentoAlocacaoDTO {
}

