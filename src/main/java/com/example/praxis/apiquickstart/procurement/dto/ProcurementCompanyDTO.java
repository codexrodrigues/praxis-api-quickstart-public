package com.example.praxis.apiquickstart.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
        name = "ProcurementCompanyDTO",
        description = "Empresa compradora cadastrada para abastecimento e contratos, com identificacao fiscal, localidade, status de elegibilidade e motivo de bloqueio quando aplicavel.")
public class ProcurementCompanyDTO {
    @Schema(description = "Identificador da empresa compradora no procurement; referencia contratos, fornecedores, produtos e pedidos relacionados.")
    private Integer id;

    @UISchema(label = "Codigo", controlType = FieldControlType.INPUT, order = 10, icon = "tag")
    @Schema(
            description = "Codigo interno ou de integracao (ERP, filial).")
    private String code;

    @UISchema(label = "Razao social", controlType = FieldControlType.INPUT, required = true, order = 20, icon = "business")
    @Schema(
            description = "Razao social ou nome comercial legal.")
    private String legalName;

    @UISchema(label = "Documento", controlType = FieldControlType.INPUT, order = 30, icon = "fingerprint")
    @Schema(
            description = "CNPJ ou documento equivalente; usado em compliance e nota.")
    private String documentNumber;

    @UISchema(label = "Cidade", controlType = FieldControlType.INPUT, order = 40, icon = "location_city")
    @Schema(
            description = "Municipio sede para logistica e fisco.")
    private String city;

    @UISchema(label = "UF", controlType = FieldControlType.INPUT, order = 50, icon = "map")
    @Schema(
            description = "Unidade federativa (sigla).")
    private String state;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, order = 60, icon = "toggle_on")
    @Schema(
            description = "Elegibilidade em compras (ACTIVE, INACTIVE, BLOCKED).")
    private String status;

    @UISchema(label = "Motivo de bloqueio", controlType = FieldControlType.INPUT, order = 70, icon = "notes")
    @Schema(
            description = "Texto de auditoria quando status impede novos pedidos ou contratos.")
    private String disabledReason;
}
