package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;

import java.time.OffsetDateTime;

@Schema(
        name = "MissaoWorkflowResultDTO",
        description = "Resposta de workflow de missao: fase anterior e atual, justificativa, instante da transicao, "
                + "janela executada (inicio/fim real quando aplicavel) e mensagem para a UI. OpenAPI 3.1 (demo).")
public class MissaoWorkflowResultDTO {

    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;
    @Schema(
            description = "MissaoStatus antes do comando; delta de auditoria.")
    private MissaoStatus statusAnterior;
    @Schema(
            description = "MissaoStatus apos persistencia; fonte de verdade para detalhe e dashboards.")
    private MissaoStatus statusAtual;
    @Schema(
            description = "Eco da justificativa enviada na requisicao (quando guardada).")
    private String justificativa;
    @Schema(
            description = "Instante associado a transicao (informado ou gerado pelo servidor).")
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


