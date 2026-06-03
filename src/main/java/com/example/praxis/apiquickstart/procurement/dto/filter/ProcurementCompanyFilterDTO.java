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
        name = "ProcurementCompanyFilterDTO",
        description = "Criterios de busca em empresas legais (entidade de compras; nao e a empresa a editar so por filtrar). "
                + "Usado com GenericFilter / POST /filter no demo Procurement (cnpj, razao, status) (demo).")
public class ProcurementCompanyFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, order = 10, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Razao social ou nome comercial; LIKE (demo).")
    private String legalName;

    @UISchema(controlType = FieldControlType.INPUT, order = 20, icon = "fingerprint")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "CPF/ CNPJ/ documento (formatacao livre na busca); LIKE (demo).")
    private String documentNumber;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao cadastral/ habilitacao (catalogo de status); EQUAL string (demo).")
    private String status;
}
