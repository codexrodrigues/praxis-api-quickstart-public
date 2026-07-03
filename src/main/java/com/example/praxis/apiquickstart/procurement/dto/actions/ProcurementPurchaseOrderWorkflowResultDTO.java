package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "ProcurementPurchaseOrderWorkflowResultDTO",
        description = "Resultado da decisao de ciclo de vida do pedido, incluindo status anterior, status atual, datas operacionais e justificativa.")
public class ProcurementPurchaseOrderWorkflowResultDTO {

    @Schema(description = "Identificador do pedido afetado pela decisao.")
    private Integer id;
    @Schema(description = "Fornecedor vinculado ao pedido.")
    private Integer supplierId;
    @Schema(description = "Contrato que governa o pedido, quando existir.")
    private Integer contractId;
    @Schema(description = "Produto solicitado pelo pedido.")
    private Integer productId;
    @Schema(description = "Status do pedido antes da decisao.")
    private String statusAnterior;
    @Schema(description = "Status do pedido apos a persistencia da decisao.")
    private String statusAtual;
    @Schema(description = "Justificativa registrada para auditoria da decisao.")
    private String motivo;
    @Schema(description = "Data em que o pedido foi aprovado.")
    private LocalDate approvedAt;
    @Schema(description = "Data em que o pedido foi cancelado.")
    private LocalDate cancelledAt;
    @Schema(description = "Data em que o pedido foi recebido.")
    private LocalDate receivedAt;
    @Schema(description = "Mensagem amigavel para operador, cockpit e consumidores semanticos.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
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

    public LocalDate getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDate approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDate getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDate cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDate receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
