package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "missaoTitulo"})
@Schema(name = "CreateMissaoEventoDTO", description = "Comando para registrar um marco na linha do tempo de uma missao, vinculando categoria, timestamp e narrativa para auditoria, acompanhamento e debriefing.")
public class CreateMissaoEventoDTO extends MissaoEventoDTO {
}

