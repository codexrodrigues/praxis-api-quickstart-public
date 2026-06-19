package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateProcurementProductDTO",
        description = "Comando para cadastrar um item de catalogo de compras, vinculando empresa, contrato, SKU, unidade, estoque inicial e elegibilidade.")
public class CreateProcurementProductDTO extends ProcurementProductDTO {
}
