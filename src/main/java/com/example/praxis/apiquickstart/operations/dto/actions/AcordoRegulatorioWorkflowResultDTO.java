package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "AcordoRegulatorioWorkflowResultDTO",
        description = "Resposta de workflow: id do acordo, status antes e depois, justificativa e mensagem operacional para a UI.")
public class AcordoRegulatorioWorkflowResultDTO {

    @UISchema(label = "ID", controlType = FieldControlType.INPUT, readOnly = true, icon = "label")
    @Schema(
            description = "Identificador do acordo atingido pela transicao.")
    private Integer id;

    @UISchema(label = "Status anterior", controlType = FieldControlType.INPUT, readOnly = true, icon = "toggle_on")
    @Schema(
            description = "AcordoStatus antes do comando; para auditoria de delta.")
    private AcordoStatus statusAnterior;

    @UISchema(label = "Status atual", controlType = FieldControlType.INPUT, readOnly = true, icon = "toggle_on")
    @Schema(
            description = "AcordoStatus apos persistencia; fonte de verdade para tela de detalhe.")
    private AcordoStatus statusAtual;

    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, readOnly = true, icon = "notes")
    @Schema(
            description = "Eco da justificativa fornecida na requisicao (quando guardada).")
    private String justificativa;

    @UISchema(label = "Mensagem", controlType = FieldControlType.INPUT, readOnly = true, icon = "label")
    @Schema(
            description = "Texto amigavel para o operador (sucesso, alerta, proximo passo).")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AcordoStatus getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(AcordoStatus statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public AcordoStatus getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(AcordoStatus statusAtual) {
        this.statusAtual = statusAtual;
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


