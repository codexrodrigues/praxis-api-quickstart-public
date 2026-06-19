package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateLicencasOperacaoDTO", description = "Comando para revisar uma licenca operacional existente, ajustando acordo, titular, equipe, classe ou vigencia sem perder rastreabilidade da autorizacao.")
public class UpdateLicencasOperacaoDTO extends CreateLicencasOperacaoDTO {
}

