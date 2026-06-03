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
        name = "ProcurementSupplierFilterDTO",
        description = "Criterios de busca em fornecedores (master data de supply); nao e o cadastro a mutar so por filtrar. "
                + "Inclui homologacao; GenericFilter / POST /filter (demo).")
public class ProcurementSupplierFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fornecedores homologados para a empresa; EQUAL companyId (escopo) (demo).")
    private Integer companyId;

    @UISchema(controlType = FieldControlType.INPUT, order = 20, icon = "business")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Razao social; LIKE (demo).")
    private String legalName;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao operacional (ativo, bloqueado, etc.); EQUAL (demo).")
    private String status;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estagio de qualificacao (apto, em analise, reprovado); EQUAL homologationStatus (demo).")
    private String homologationStatus;
}
