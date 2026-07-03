package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id", "status", "disabledReason", "approvedAt", "cancelledAt", "receivedAt"})
@Schema(
    name = "CreateProcurementPurchaseOrderDTO",
    description = "Comando para emitir um pedido de compra vinculando empresa, fornecedor, contrato opcional, produto, quantidade, moeda e data. A selecao de fornecedor e produto deve respeitar option sources e politicas publicadas de procurement."
)
public class CreateProcurementPurchaseOrderDTO extends ProcurementPurchaseOrderDTO {
}
