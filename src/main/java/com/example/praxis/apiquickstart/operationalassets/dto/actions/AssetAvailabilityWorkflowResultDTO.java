package com.example.praxis.apiquickstart.operationalassets.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AssetAvailabilityWorkflowResultDTO",
        description = "Resultado da decisao de disponibilidade do ativo, com status anterior, status atual, motivo e mensagem operacional.")
public class AssetAvailabilityWorkflowResultDTO {

    @Schema(description = "Identificador do ativo afetado pela decisao.")
    private Integer id;
    @Schema(description = "Nome operacional do ativo para leitura humana no cockpit e auditoria.")
    private String nome;
    @Schema(description = "Tipo de ativo impactado pela action, como equipment ou vehicle.")
    private String assetType;
    @Schema(description = "Status de disponibilidade antes da decisao.")
    private String statusAnterior;
    @Schema(description = "Status de disponibilidade apos a persistencia da decisao.")
    private String statusAtual;
    @Schema(description = "Justificativa registrada para explicar a mudanca de disponibilidade.")
    private String motivo;
    @Schema(description = "Mensagem amigavel para operador, cockpit e consumidores semanticos.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
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
