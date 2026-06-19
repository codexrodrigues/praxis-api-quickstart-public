package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateProcurementCompanyDTO",
        description = "Comando para cadastrar uma empresa compradora no procurement, com identificacao fiscal, localidade e elegibilidade inicial para compras.")
public class CreateProcurementCompanyDTO extends ProcurementCompanyDTO {
}
