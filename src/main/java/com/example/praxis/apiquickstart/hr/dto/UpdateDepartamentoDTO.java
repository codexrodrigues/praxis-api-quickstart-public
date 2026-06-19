package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateDepartamentoDTO", description = "Comando para substituir os dados editaveis de uma unidade organizacional, mantendo sua identidade tecnica e recalculando leituras como nome do responsavel no retorno.")
public class UpdateDepartamentoDTO extends CreateDepartamentoDTO {
}
