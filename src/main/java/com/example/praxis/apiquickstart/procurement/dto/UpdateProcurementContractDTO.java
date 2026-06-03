package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(name = "UpdateProcurementContractDTO", description = "Corpo de atualizacao no procurement; campos mutaveis. OpenAPI 3.1 (demo).")
public class UpdateProcurementContractDTO extends ProcurementContractDTO {
}
