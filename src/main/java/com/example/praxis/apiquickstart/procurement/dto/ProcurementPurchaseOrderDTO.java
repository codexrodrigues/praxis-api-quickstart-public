package com.example.praxis.apiquickstart.procurement.dto;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Schema(
    name = "ProcurementPurchaseOrderDTO",
    description = "Pedido de compra: empresa, fornecedor, contrato, produto, quantidade e data. Cadeia depende de fornecedores e contratos homologados."
)
@Getter
@Setter
public class ProcurementPurchaseOrderDTO {
    @Schema(description = "Identificador do pedido de compra.", example = "1")
    private Integer id;

    @UISchema(
            label = "Empresa",
            controlType = FieldControlType.ENTITY_LOOKUP,
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
            valueField = "id",
            displayField = "label",
            required = true,
            order = 10,
            icon = "business"
    )
    @Schema(
        description = "Empresa compradora; filtra fornecedores, contratos e regras fiscais aplicaveis."
    )
    private Integer companyId;

    @UISchema(
            label = "Fornecedor",
            controlType = FieldControlType.ENTITY_LOOKUP,
            endpoint = ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS,
            valueField = "id",
            displayField = "label",
            description = "Lookup governado pela option-source supplier: apenas fornecedores ACTIVE ou APPROVED podem ser selecionados; INACTIVE e BLOCKED ficam indisponiveis.",
            dependentField = "companyId",
            resetOnDependentChange = true,
            required = true,
            order = 20,
            icon = "storefront"
    )
    @Schema(
        description = "Fornecedor (lookup governado: apenas fornecedores ativos/homologados no contexto da empresa)."
    )
    private Integer supplierId;

    @UISchema(
            label = "Contrato",
            controlType = FieldControlType.ENTITY_LOOKUP,
            endpoint = ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS,
            valueField = "id",
            displayField = "label",
            dependentField = "supplierId",
            resetOnDependentChange = true,
            order = 30,
            icon = "contract"
    )
    @Schema(description = "Contrato de abastecimento com o fornecedor, quando a politica de compra exigir vinculacao legal.")
    private Integer contractId;

    @UISchema(
            label = "Produto",
            controlType = FieldControlType.ENTITY_LOOKUP,
            endpoint = ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_OPTIONS,
            valueField = "id",
            displayField = "label",
            dependentField = "contractId",
            resetOnDependentChange = true,
            required = true,
            order = 40,
            icon = "inventory_2"
    )
    @Schema(description = "Item de catalogo a adquirir, respeitando precos e unidades do contrato.")
    private Integer productId;

    @Schema(description = "Data de emissao do pedido; ancora a competencia de estoque e faturamento.")
    @UISchema(label = "Data do pedido", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 50, icon = "event")
    private LocalDate orderDate;

    @Schema(description = "Codigo ISO da moeda do pedido (ex. BRL, USD), alinhado a regras de arredondamento do ERP.")
    @UISchema(label = "Moeda", controlType = FieldControlType.INPUT, order = 60, icon = "payments")
    private String currency;

    @Schema(description = "Quantidade encomendada na unidade de medida do produto, sujeita a minimo e multiplos de embalagem.")
    @UISchema(label = "Quantidade", type = FieldDataType.NUMBER, order = 70, icon = "format_list_numbered")
    private Integer quantity;

    @Schema(description = "Estado operacional do pedido no ciclo de suprimentos: rascunho, aprovado, cancelado ou recebido.")
    @UISchema(
            label = "Status do pedido",
            controlType = FieldControlType.SELECT,
            order = 80,
            icon = "published_with_changes"
    )
    private String status;

    @Schema(description = "Motivo que explica por que o pedido deixou de estar elegivel para execucao.")
    @UISchema(label = "Motivo de bloqueio", controlType = FieldControlType.TEXTAREA, order = 90, icon = "notes")
    private String disabledReason;

    @Schema(description = "Data em que o pedido foi aprovado para execucao operacional.")
    @UISchema(label = "Aprovado em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 100, icon = "verified")
    private LocalDate approvedAt;

    @Schema(description = "Data em que o pedido foi cancelado e retirado do fluxo de compra.")
    @UISchema(label = "Cancelado em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110, icon = "cancel")
    private LocalDate cancelledAt;

    @Schema(description = "Data em que o pedido foi recebido, materializando fechamento do fluxo de suprimentos.")
    @UISchema(label = "Recebido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 120, icon = "inventory")
    private LocalDate receivedAt;
}
