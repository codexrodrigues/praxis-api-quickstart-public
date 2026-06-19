package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateIndenizacaoDTO", description = "Comando para revisar valor, estado de pagamento ou dados de processo de uma indenizacao existente, mantendo a relacao com o incidente de origem.")
public class UpdateIndenizacaoDTO extends CreateIndenizacaoDTO {
}
