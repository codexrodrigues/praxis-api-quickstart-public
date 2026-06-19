package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "BaseAcessoWorkflowResultDTO",
        description = "Resultado da decisao de acesso a base, indicando a elegibilidade anterior e vigente, o nivel aprovado, "
                + "a justificativa registrada e a mensagem operacional da decisao.")
public class BaseAcessoWorkflowResultDTO {

    @Schema(description = "Identificador do registro de acesso afetado pela decisao; referencia o recurso em URLs e relacionamentos.")
    private Integer id;
    @Schema(
            description = "Situacao de elegibilidade antes da transicao (ex. credencial suspensa).")
    private Boolean ativoAnterior;
    @Schema(
            description = "Situacao de elegibilidade apos persistencia; controla novos acessos fisicos.")
    private Boolean ativoAtual;
    @Schema(
            description = "String de classificacao de corredor vigente apos a decisao (ex. TSI-2).")
    private String nivelAcesso;
    @Schema(
            description = "Justificativa preservada para explicar a ativacao ou desativacao da credencial.")
    private String justificativa;
    @Schema(
            description = "Texto amigavel para o operador (sucesso, alerta, proximo passo).")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getAtivoAnterior() {
        return ativoAnterior;
    }

    public void setAtivoAnterior(Boolean ativoAnterior) {
        this.ativoAnterior = ativoAnterior;
    }

    public Boolean getAtivoAtual() {
        return ativoAtual;
    }

    public void setAtivoAtual(Boolean ativoAtual) {
        this.ativoAtual = ativoAtual;
    }

    public String getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(String nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}

