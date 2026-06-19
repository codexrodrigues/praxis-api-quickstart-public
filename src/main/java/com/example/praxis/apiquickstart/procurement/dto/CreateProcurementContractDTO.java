package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateProcurementContractDTO",
        description = "Comando para cadastrar um contrato de fornecimento, vinculando empresa, fornecedor, numero legal, moeda, vigencia e status inicial.")
public class CreateProcurementContractDTO extends ProcurementContractDTO {
}
