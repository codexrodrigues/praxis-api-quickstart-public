package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateMissaoEventoDTO", description = "Comando para revisar um evento de missao existente, preservando sua identidade e ajustando categoria, momento ou narrativa operacional.")
public class UpdateMissaoEventoDTO extends CreateMissaoEventoDTO {
}

