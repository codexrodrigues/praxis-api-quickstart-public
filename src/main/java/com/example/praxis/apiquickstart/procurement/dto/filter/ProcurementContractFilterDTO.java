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
        name = "ProcurementContractFilterDTO",
        description = "Criterios de busca em contratos de compra (nao e o contrato a ajustar so com filtrar). "
                + "Navegacao: empresa, fornecedor, numero, status, moeda. GenericFilter / POST /filter (demo).")
public class ProcurementContractFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Empresa contratante; EQUAL companyId (FK) (demo).")
    private Integer companyId;

    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 20, icon = "storefront")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fornecedor/ contratado; EQUAL supplierId (FK) (demo).")
    private Integer supplierId;

    @UISchema(controlType = FieldControlType.INPUT, order = 30, icon = "tag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Numero/ referencia de contrato; LIKE (demo).")
    private String number;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ciclo de vida (rascunho, ativo, encerrado, etc.); EQUAL (demo).")
    private String status;

    @UISchema(controlType = FieldControlType.INPUT, order = 50, icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Codigo ISO de moeda (BRL, USD, etc.); EQUAL (demonstrativo) (demo).")
    private String currency;
}
