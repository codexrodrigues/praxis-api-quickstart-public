package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
    name = "CreateProcurementSupplierDTO",
    description = "Comando para cadastrar fornecedor no contexto de uma empresa compradora, documentando razao social, documento, homologacao, risco, status e motivo de bloqueio quando aplicavel. Esses campos governam elegibilidade em lookups e pedidos de compra."
)
public class CreateProcurementSupplierDTO extends ProcurementSupplierDTO {
}
