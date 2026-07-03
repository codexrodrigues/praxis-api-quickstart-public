package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ProcurementSupplierWorkflowResultDTO",
        description = "Resultado da decisao de elegibilidade do fornecedor, incluindo status anterior, status atual, motivo e mensagem operacional.")
public class ProcurementSupplierWorkflowResultDTO {

    @Schema(description = "Identificador do fornecedor afetado pela decisao.")
    private Integer id;
    @Schema(description = "Codigo operacional do fornecedor usado em pedidos, contratos e conciliacoes.")
    private String supplierCode;
    @Schema(description = "Razao social do fornecedor para leitura humana em auditoria e cockpit.")
    private String legalName;
    @Schema(description = "Status de elegibilidade antes da decisao.")
    private String statusAnterior;
    @Schema(description = "Status de elegibilidade apos a persistencia da decisao.")
    private String statusAtual;
    @Schema(description = "Justificativa registrada para explicar bloqueio ou reabilitacao.")
    private String motivo;
    @Schema(description = "Mensagem amigavel para operador, cockpit e consumidores semanticos.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(String statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
