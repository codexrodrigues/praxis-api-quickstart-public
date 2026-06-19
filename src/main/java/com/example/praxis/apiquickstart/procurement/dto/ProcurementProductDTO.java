package com.example.praxis.apiquickstart.procurement.dto;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
        name = "ProcurementProductDTO",
        description = "Item de catalogo de compras vinculado a empresa e contrato, com SKU, categoria, estoque disponivel, unidade de medida e elegibilidade para requisicao.")
public class ProcurementProductDTO {
    @Schema(description = "Identificador do item de catalogo; referencia linhas de pedido, contrato vigente e relacionamentos de procurement.")
    private Integer id;

    @UISchema(label = "Empresa", controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business",
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label")
    @Schema(
            description = "FK; tenant ou empresa detentora do catalogo (companyId).")
    private Integer companyId;

    @UISchema(label = "Contrato", controlType = FieldControlType.ENTITY_LOOKUP, order = 20, icon = "contract",
            endpoint = ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "companyId", resetOnDependentChange = true)
    @Schema(
            description = "FK; contrato que rege preco e disponibilidade (contractId).")
    private Integer contractId;

    @UISchema(label = "SKU", controlType = FieldControlType.INPUT, required = true, order = 30, icon = "inventory_2")
    @Schema(
            description = "Codigo de item alfanumerico (pedido, XML, conciliacao).")
    private String sku;

    @UISchema(label = "Produto", controlType = FieldControlType.INPUT, required = true, order = 40, icon = "inventory_2")
    @Schema(
            description = "Denominacao comercial exibida em compras e recebimento.")
    private String name;

    @UISchema(label = "Categoria", controlType = FieldControlType.INPUT, order = 50, icon = "category")
    @Schema(
            description = "Categoria de linha para aprovacoes e relatorios.")
    private String categoryName;

    @UISchema(label = "Estoque", type = FieldDataType.NUMBER, order = 60, icon = "inventory_2")
    @Schema(
            description = "Quantidade disponivel na unidade de medida do item (saldos de demonstracao).")
    private Integer stockAvailable;

    @UISchema(label = "Unidade", controlType = FieldControlType.INPUT, order = 70, icon = "inventory_2")
    @Schema(
            description = "Unidade de medida (ex. UN, CX); amarra a quantidade de pedido.")
    private String unitOfMeasure;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, order = 80, icon = "toggle_on")
    @Schema(
            description = "Elegibilidade do item (ativo, inativo, bloqueado, etc.).")
    private String status;

    @UISchema(label = "Motivo de bloqueio", controlType = FieldControlType.INPUT, order = 90, icon = "notes")
    @Schema(
            description = "Explicacao quando o item nao pode ser requisitado.")
    private String disabledReason;
}
