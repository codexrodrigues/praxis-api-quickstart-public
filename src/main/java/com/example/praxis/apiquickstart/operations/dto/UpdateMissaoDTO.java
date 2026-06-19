package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
    name = "UpdateMissaoDTO",
    description = "Comando para atualizar os dados editaveis da missao sem alterar sua identidade. Mudancas em objetivo, prioridade, status, local, ameaca e datas podem afetar planejamento, dashboards e regras governadas de operacao."
)
public class UpdateMissaoDTO extends CreateMissaoDTO {
}

