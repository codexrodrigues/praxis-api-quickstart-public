package com.example.praxis.apiquickstart.procurement.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
        name = "ProcurementPurchaseOrderFilterDTO",
        description = "Criterios de busca em pedidos de compra (PO); nao e o PO a editar so com filtrar. "
                + "Usado para localizar pedidos por empresa compradora, fornecedor, contrato base e item requisitado.")
public class ProcurementPurchaseOrderFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Empresa compradora", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            helpText = "Filtra pedidos de compra emitidos por uma empresa.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Empresa compradora que emitiu ou administra o pedido de compra.")
    private Integer companyId;

    @UISchema(label = "Fornecedor", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            endpoint = ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "companyId", resetOnDependentChange = true,
            helpText = "Mostra pedidos de compra direcionados a um fornecedor específico.", icon = "storefront")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fornecedor destinatario ou contratado no pedido de compra.")
    private Integer supplierId;

    @UISchema(label = "Contrato", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            endpoint = ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "supplierId", resetOnDependentChange = true,
            helpText = "Filtra pedidos vinculados a um contrato base.", icon = "contract")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Contrato base que governa condicoes comerciais do pedido.")
    private Integer contractId;

    @UISchema(label = "Produto ou serviço", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 40,
            endpoint = ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "contractId", resetOnDependentChange = true,
            helpText = "Filtra pedidos que incluem um produto ou serviço específico.", icon = "inventory_2")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Produto ou servico requisitado pelo pedido de compra.")
    private Integer productId;

    @UISchema(label = "Status do pedido", controlType = FieldControlType.SELECT, order = 50,
            helpText = "Filtra pedidos por etapa do ciclo de compra: rascunho, aprovado, cancelado ou recebido.",
            icon = "published_with_changes")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado operacional do pedido no fluxo de suprimentos: DRAFT, APPROVED, CANCELLED ou RECEIVED.")
    private String status;
}
