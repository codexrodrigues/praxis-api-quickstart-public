package com.example.praxis.apiquickstart.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
    name = "UpdateProcurementSupplierDTO",
    description = "Comando para atualizar fornecedor e recalibrar sua elegibilidade operacional. Alteracoes de homologacao, risco, status ou motivo de bloqueio impactam option sources, pedidos de compra e regras governadas de selecao."
)
public class UpdateProcurementSupplierDTO extends ProcurementSupplierDTO {
}
