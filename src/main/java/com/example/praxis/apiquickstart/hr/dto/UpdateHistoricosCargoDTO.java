package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateHistoricosCargoDTO", description = "Comando para revisar uma movimentacao de cargo existente, preservando sua identidade e ajustando cargo, vigencia ou observacoes de carreira.")
public class UpdateHistoricosCargoDTO extends CreateHistoricosCargoDTO {
}
