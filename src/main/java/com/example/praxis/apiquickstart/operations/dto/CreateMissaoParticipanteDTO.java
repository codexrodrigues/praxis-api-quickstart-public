package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "missaoTitulo", "funcionarioNome"})
@Schema(name = "CreateMissaoParticipanteDTO", description = "Comando para incluir um colaborador na equipe de uma missão, definindo papel, ordem de atuação, liderança e resultado esperado.")
public class CreateMissaoParticipanteDTO extends MissaoParticipanteDTO {
}
