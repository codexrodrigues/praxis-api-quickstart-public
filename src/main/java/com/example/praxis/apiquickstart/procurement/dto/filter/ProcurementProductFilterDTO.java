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
        name = "ProcurementProductFilterDTO",
        description = "Criterios de busca no catalogo de produtos/ itens de contrato (sku, servico); nao e o item a persistir so por filtrar. "
                + "Usado para localizar itens por empresa, contrato, denominacao e elegibilidade para requisicao.")
public class ProcurementProductFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Empresa", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            helpText = "Filtra itens do catálogo por empresa compradora.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Empresa compradora proprietaria ou escopo do item de catalogo.")
    private Integer companyId;

    @UISchema(label = "Contrato", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            endpoint = ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            dependentField = "companyId", resetOnDependentChange = true,
            helpText = "Mostra produtos ou serviços associados a um contrato específico.", icon = "contract")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Contrato que rege disponibilidade, preco e condicoes do item.")
    private Integer contractId;

    @UISchema(label = "Produto ou serviço", controlType = FieldControlType.INPUT, order = 30,
            helpText = "Busca por nome comercial, técnico ou SKU do item.", icon = "inventory_2")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome comercial, denominacao tecnica ou SKU do produto ou servico.")
    private String name;

    @UISchema(label = "Status do item", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra itens ativos, inativos ou descontinuados.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status de elegibilidade do item, como ativo, inativo, bloqueado ou descontinuado.")
    private String status;
}
