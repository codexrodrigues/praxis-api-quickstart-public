package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateMissaoParticipanteDTO", description = "Comando para revisar a participação em uma missão, ajustando papel, liderança, ordem ou resultado sem alterar a identidade do vínculo.")
public class UpdateMissaoParticipanteDTO extends CreateMissaoParticipanteDTO {
}
