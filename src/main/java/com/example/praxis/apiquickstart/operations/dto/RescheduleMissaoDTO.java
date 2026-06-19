package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "RescheduleMissaoDTO",
        description = "Corpo de reagendamento: novo teatro, janela prevista e objetivo tatico atualizado. "
                + "Ajusta planejamento de execucao e logistica sem registrar inicio, conclusao ou falha da missao.")
public class RescheduleMissaoDTO {

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Local", controlType = FieldControlType.INPUT, required = true, maxLength = 200, order = 10, icon = "location_on")
    @Schema(
            description = "Onde a missao devera ocorrer apos a mudanca (texto de brief).")
    private String local;

    @NotNull
    @UISchema(label = "Início Previsto", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, order = 20, icon = "event")
    @Schema(
            description = "Novo inicio de janela planejada (planejada, nao executada).")
    private OffsetDateTime inicioPrev;

    @NotNull
    @UISchema(label = "Fim Previsto", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, order = 30, icon = "event")
    @Schema(
            description = "Novo fim de janela planejada; ancora a logistica e debrief previsto.")
    private OffsetDateTime fimPrev;

    @Size(max = 4000)
    @UISchema(label = "Objetivo", controlType = FieldControlType.TEXTAREA, maxLength = 4000, order = 40, icon = "flag")
    @Schema(
            description = "Revisao do objetivo e ROE apos reagendamento; opcional.")
    private String objetivo;

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public OffsetDateTime getInicioPrev() {
        return inicioPrev;
    }

    public void setInicioPrev(OffsetDateTime inicioPrev) {
        this.inicioPrev = inicioPrev;
    }

    public OffsetDateTime getFimPrev() {
        return fimPrev;
    }

    public void setFimPrev(OffsetDateTime fimPrev) {
        this.fimPrev = fimPrev;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(String objetivo) {
        this.objetivo = objetivo;
    }
}

