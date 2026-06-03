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
        name = "ProcurementPurchaseOrderFilterDTO",
        description = "Criterios de busca em pedidos de compra (PO); nao e o PO a editar so com filtrar. "
                + "Recorte por empresa, fornecedor, contrato e produto; GenericFilter / POST /filter (demo).")
public class ProcurementPurchaseOrderFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Organizacao compradora; EQUAL companyId (demo).")
    private Integer companyId;

    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 20, icon = "storefront")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fornecedor; EQUAL supplierId (demo).")
    private Integer supplierId;

    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 30, icon = "contract")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Contrato base; EQUAL contractId (demo).")
    private Integer contractId;

    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 40, icon = "inventory_2")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Linha de produto; EQUAL productId (demo).")
    private Integer productId;
}
