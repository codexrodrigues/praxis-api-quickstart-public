package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id", "status", "disabledReason", "approvedAt", "cancelledAt", "receivedAt"})
@Schema(
    name = "UpdateProcurementPurchaseOrderDTO",
    description = "Comando para revisar um pedido de compra existente, preservando sua identidade e reavaliando empresa, fornecedor, contrato, produto, quantidade, moeda e data contra as politicas governadas de procurement."
)
public class UpdateProcurementPurchaseOrderDTO extends ProcurementPurchaseOrderDTO {
}
