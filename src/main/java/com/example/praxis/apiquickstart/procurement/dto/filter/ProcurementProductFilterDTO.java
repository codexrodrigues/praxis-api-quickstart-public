package com.example.praxis.apiquickstart.procurement.dto.filter;

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
                + "GenericFilter / POST /filter (demo).")
public class ProcurementProductFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Escopo por empresa; EQUAL companyId (demo).")
    private Integer companyId;

    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 20, icon = "contract")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Itens de um contrato; EQUAL contractId (demo).")
    private Integer contractId;

    @UISchema(controlType = FieldControlType.INPUT, order = 30, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Denominacao comercial ou tecnica; LIKE (demo).")
    private String name;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ativo/ inativo/ descontinuado; EQUAL status (demo).")
    private String status;
}
