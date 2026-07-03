package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ProcurementContractWorkflowResultDTO",
        description = "Resultado da decisao de ciclo de vida do contrato, incluindo status anterior, status atual e justificativa operacional.")
public class ProcurementContractWorkflowResultDTO {

    @Schema(description = "Identificador do contrato afetado pela decisao.")
    private Integer id;
    @Schema(description = "Numero legal ou operacional do contrato de fornecimento.")
    private String contractNumber;
    @Schema(description = "Nome do fornecedor vinculado ao contrato.")
    private String supplierName;
    @Schema(description = "Status do contrato antes da decisao.")
    private String statusAnterior;
    @Schema(description = "Status do contrato apos a persistencia da decisao.")
    private String statusAtual;
    @Schema(description = "Justificativa registrada para auditoria da decisao.")
    private String motivo;
    @Schema(description = "Mensagem amigavel para operador, cockpit e consumidores semanticos.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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
