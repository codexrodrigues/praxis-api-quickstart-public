package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "BaseAcessoWorkflowResultDTO",
        description = "Resposta de workflow de acesso a base: id do registo, credencial ativa antes e depois, nivel aprovado, "
                + "eco de justificativa e mensagem para a UI. OpenAPI 3.1 (demo).")
public class BaseAcessoWorkflowResultDTO {

    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
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
            description = "Eco da justificativa fornecida na requisicao de workflow (quando armazenada).")
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

