package com.example.praxis.apiquickstart.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Getter
@Setter
@Schema(
        name = "ProcurementContractDTO",
        description = "Contrato de fornecimento (empresa, fornecedor, vigencia, moeda, status). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class ProcurementContractDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @UISchema(label = "Empresa", controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business")
    @Schema(
            description = "FK; empresa compradora (companyId).")
    private Integer companyId;

    @UISchema(label = "Fornecedor", controlType = FieldControlType.ENTITY_LOOKUP, order = 20, icon = "storefront")
    @Schema(
            description = "FK; fornecedor contratado (supplierId).")
    private Integer supplierId;

    @UISchema(label = "Numero", controlType = FieldControlType.INPUT, required = true, order = 30, icon = "pin")
    @Schema(
            description = "Numero legal do contrato ou aditivo mestre.")
    private String number;

    @UISchema(label = "Fornecedor", controlType = FieldControlType.INPUT, order = 40, icon = "storefront")
    @Schema(
            description = "Nome do fornecedor denormalizado para listagem e busca (read model).")
    private String supplierName;

    @UISchema(label = "Moeda", controlType = FieldControlType.INPUT, order = 50, icon = "payments")
    @Schema(
            description = "Codigo ISO da moeda do contrato (ex. BRL, USD).")
    private String currency;

    @UISchema(label = "Valido ate", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 60, icon = "event")
    @Schema(
            description = "Fim de vigencia; precos e pedidos obedecem a esta data.")
    private LocalDate validUntil;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, order = 70, icon = "toggle_on")
    @Schema(
            description = "Ciclo de vida do contrato (ativo, inativo, bloqueado, etc.).")
    private String status;

    @UISchema(label = "Motivo de bloqueio", controlType = FieldControlType.INPUT, order = 80, icon = "notes")
    @Schema(
            description = "Motivacao de bloqueio ou suspensao para auditoria.")
    private String disabledReason;
}
