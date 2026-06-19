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
        name = "ProcurementContractFilterDTO",
        description = "Criterios de busca em contratos de compra (nao e o contrato a ajustar so com filtrar). "
                + "Usado para navegar contratos por empresa, fornecedor, numero, status e moeda.")
public class ProcurementContractFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Empresa contratante", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            helpText = "Filtra contratos vinculados a uma empresa compradora.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Empresa compradora contratante vinculada ao contrato de fornecimento.")
    private Integer companyId;

    @UISchema(label = "Fornecedor", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            endpoint = ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "companyId", resetOnDependentChange = true,
            helpText = "Mostra contratos firmados com um fornecedor específico.", icon = "storefront")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fornecedor contratado ao qual o contrato de fornecimento esta vinculado.")
    private Integer supplierId;

    @UISchema(label = "Número do contrato", controlType = FieldControlType.INPUT, order = 30,
            helpText = "Busca por número, código ou referência do contrato.", icon = "tag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do numero legal, codigo ou referencia interna do contrato.")
    private String number;

    @UISchema(label = "Status do contrato", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra contratos por ciclo de vida, como rascunho, ativo ou encerrado.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status do ciclo de vida do contrato, como rascunho, ativo, suspenso ou encerrado.")
    private String status;

    @UISchema(label = "Moeda", controlType = FieldControlType.INPUT, order = 50,
            helpText = "Filtra por código de moeda do contrato, como BRL ou USD.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Codigo ISO da moeda contratual usada para precos, pedidos e conciliacao.")
    private String currency;
}
