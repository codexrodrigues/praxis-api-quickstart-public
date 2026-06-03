package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "MissaoWorkflowRequestDTO",
        description = "Requisicao de transicao de workflow de missao: justificativa e instante (opcional) da mudanca de fase.")
public class MissaoWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(
            description = "Razao da mudanca de status (aprovacao, cancelamento, retorno, etc.).")
    private String justificativa;

    @UISchema(label = "Momento da transição", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, order = 20, icon = "event")
    @Schema(
            description = "Se informado, ancora a linha de tempo; caso contrario o servidor grava o instante de processamento.")
    private OffsetDateTime ocorridoEm;

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
}

