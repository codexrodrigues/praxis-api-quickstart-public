package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "UpdateProcurementProductDTO",
        description = "Comando para revisar um item de catalogo de compras, incluindo contrato, estoque, unidade, status ou motivo de bloqueio.")
public class UpdateProcurementProductDTO extends ProcurementProductDTO {
}
