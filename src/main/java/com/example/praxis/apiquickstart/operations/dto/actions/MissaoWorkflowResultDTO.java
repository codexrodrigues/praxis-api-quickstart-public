package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;

import java.time.OffsetDateTime;

@Schema(
        name = "MissaoWorkflowResultDTO",
        description = "Resultado persistido da transicao de missao, com fase anterior, fase atual, justificativa auditavel, "
                + "marco temporal executado e janela operacional real quando a decisao altera inicio ou encerramento da missao.")
public class MissaoWorkflowResultDTO {

    @Schema(description = "Identificador da missao afetada pela transicao; usado para atualizar a tela de detalhe e correlacionar eventos de auditoria.")
    private Integer id;
    @Schema(
            description = "MissaoStatus antes do comando; delta de auditoria.")
    private MissaoStatus statusAnterior;
    @Schema(
            description = "MissaoStatus apos persistencia; fonte de verdade para detalhe e dashboards.")
    private MissaoStatus statusAtual;
    @Schema(
            description = "Justificativa efetivamente associada a transicao, preservada para auditoria e revisao operacional.")
    private String justificativa;
    @Schema(
            description = "Instante de negocio registrado para a transicao, informado pelo cliente ou definido pelo servidor.")
    private OffsetDateTime ocorridoEm;
    @Schema(
            description = "Inicio executado quando a fase fixa linha de tempo real; opcional.")
    private OffsetDateTime inicioReal;
    @Schema(
            description = "Fim executado quando a fase encerra a operacao no teatro; opcional.")
    private OffsetDateTime fimReal;
    @Schema(
            description = "Texto amigavel para o operador (sucesso, bloqueio, proxima acao).")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MissaoStatus getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(MissaoStatus statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public MissaoStatus getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(MissaoStatus statusAtual) {
        this.statusAtual = statusAtual;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(OffsetDateTime ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public OffsetDateTime getInicioReal() {
        return inicioReal;
    }

    public void setInicioReal(OffsetDateTime inicioReal) {
        this.inicioReal = inicioReal;
    }

    public OffsetDateTime getFimReal() {
        return fimReal;
    }

    public void setFimReal(OffsetDateTime fimReal) {
        this.fimReal = fimReal;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}


