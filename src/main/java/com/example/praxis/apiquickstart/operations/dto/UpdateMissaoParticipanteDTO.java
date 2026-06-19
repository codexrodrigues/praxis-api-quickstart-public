package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateMissaoParticipanteDTO", description = "Comando para revisar a escala de participante em missao, ajustando papel, lideranca, ordem ou resultado sem alterar a identidade tecnica do vinculo.")
public class UpdateMissaoParticipanteDTO extends CreateMissaoParticipanteDTO {
}

