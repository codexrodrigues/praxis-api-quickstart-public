package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(name = "CreateProcurementCompanyDTO", description = "Corpo de criacao no procurement; campos do POST. OpenAPI 3.1 (demo).")
public class CreateProcurementCompanyDTO extends ProcurementCompanyDTO {
}
