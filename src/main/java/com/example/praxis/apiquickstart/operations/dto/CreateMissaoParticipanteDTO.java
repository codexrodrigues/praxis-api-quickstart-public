package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "missaoTitulo", "funcionarioNome"})
@Schema(name = "CreateMissaoParticipanteDTO", description = "Comando para escalar colaborador em uma missao, definindo papel, ordem, lideranca e resultado esperado para composicao operacional.")
public class CreateMissaoParticipanteDTO extends MissaoParticipanteDTO {
}

