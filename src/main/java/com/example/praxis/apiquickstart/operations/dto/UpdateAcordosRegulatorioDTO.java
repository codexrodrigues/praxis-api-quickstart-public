package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
    name = "UpdateAcordosRegulatorioDTO",
    description = "Comando para atualizar os dados editaveis de um acordo regulatorio sem perder sua identidade de catalogo. Mudancas de jurisdicao, descricao, vigencia ou status podem alterar elegibilidade operacional, surfaces de compliance e trilhas de workflow."
)
public class UpdateAcordosRegulatorioDTO extends CreateAcordosRegulatorioDTO {
}

