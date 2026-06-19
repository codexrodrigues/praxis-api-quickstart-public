package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateMencoesMidiaDTO", description = "Comando para revisar uma mencao de midia existente, ajustando classificacao, fonte ou referencia temporal sem alterar sua identidade tecnica.")
public class UpdateMencoesMidiaDTO extends CreateMencoesMidiaDTO {
}
