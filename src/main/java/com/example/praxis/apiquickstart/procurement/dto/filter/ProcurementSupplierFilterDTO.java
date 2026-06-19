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
        name = "ProcurementSupplierFilterDTO",
        description = "Criterios de busca em fornecedores (master data de supply); nao e o cadastro a mutar so por filtrar. "
                + "Usado para localizar fornecedores por empresa, razao social, status operacional e homologacao.")
public class ProcurementSupplierFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Empresa compradora", controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label",
            helpText = "Filtra fornecedores habilitados para uma empresa específica.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Empresa compradora para a qual o fornecedor esta cadastrado ou homologado.")
    private Integer companyId;

    @UISchema(label = "Razão social", controlType = FieldControlType.INPUT, order = 20,
            helpText = "Busca fornecedores pelo nome legal ou comercial.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da razao social ou nome comercial usado para localizar o fornecedor.")
    private String legalName;

    @UISchema(label = "Status do fornecedor", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Mostra fornecedores conforme situação operacional no cadastro.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status operacional do fornecedor, usado para separar fornecedores ativos, inativos ou bloqueados.")
    private String status;

    @UISchema(label = "Status de homologação", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra fornecedores por etapa de qualificação ou aprovação.", icon = "verified")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estagio de qualificacao ou aprovacao do fornecedor no processo de homologacao.")
    private String homologationStatus;
}
