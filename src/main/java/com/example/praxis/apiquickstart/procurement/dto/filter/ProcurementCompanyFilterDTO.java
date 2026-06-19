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
                + "Usado para localizar empresas compradoras por nome, documento fiscal e situacao cadastral.")
public class ProcurementCompanyFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Razão social", controlType = FieldControlType.INPUT, order = 10,
            helpText = "Busca empresas pelo nome legal ou comercial.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da razao social ou nome comercial usado para localizar a empresa compradora.")
    private String legalName;

    @UISchema(label = "Documento", controlType = FieldControlType.INPUT, order = 20,
            helpText = "Busca por CNPJ, CPF ou outro identificador cadastral.", icon = "fingerprint")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do CNPJ, CPF ou identificador cadastral equivalente, aceitando formatacao livre na busca.")
    private String documentNumber;

    @UISchema(label = "Status da empresa", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Filtra empresas conforme situação cadastral ou habilitação.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao cadastral ou habilitacao da empresa para compras, conforme catalogo de status do procurement.")
    private String status;
}
