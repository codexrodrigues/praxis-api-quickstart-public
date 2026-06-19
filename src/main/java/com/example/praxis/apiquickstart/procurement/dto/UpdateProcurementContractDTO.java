package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "UpdateProcurementContractDTO",
        description = "Comando para revisar um contrato de fornecimento quando mudam vigencia, fornecedor, moeda, status ou motivo de bloqueio.")
public class UpdateProcurementContractDTO extends ProcurementContractDTO {
}
