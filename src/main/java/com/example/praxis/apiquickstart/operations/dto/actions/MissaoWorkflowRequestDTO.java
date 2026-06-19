package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "MissaoWorkflowRequestDTO",
        description = "Comando operacional para registrar a decisao que move uma missao entre fases controladas. "
                + "Carrega a justificativa auditavel da decisao e, quando necessario, o instante que ancora a linha de tempo real da operacao.")
public class MissaoWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(
            description = "Motivo informado pelo operador para iniciar, pausar, retomar, concluir ou falhar a missao; usado como rastro de auditoria e contexto para revisao operacional.")
    private String justificativa;

    @UISchema(label = "Momento da transição", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, order = 20, icon = "event")
    @Schema(
            description = "Instante de negocio atribuido a transicao. Quando ausente, o servidor usa o momento de processamento como marco auditavel da mudanca de fase.")
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

