package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "UpdateProcurementCompanyDTO",
        description = "Comando para revisar uma empresa compradora existente, incluindo dados fiscais, localidade, status e motivo de bloqueio.")
public class UpdateProcurementCompanyDTO extends ProcurementCompanyDTO {
}
