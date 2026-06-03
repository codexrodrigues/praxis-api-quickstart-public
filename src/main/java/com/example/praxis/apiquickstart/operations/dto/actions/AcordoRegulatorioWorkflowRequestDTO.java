package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "AcordoRegulatorioWorkflowRequestDTO",
        description = "Requisicao de transicao de workflow de acordo regulatorio: justificativa de mudanca de status.")
public class AcordoRegulatorioWorkflowRequestDTO {

    @NotBlank
    @Size(max = 500)
    @UISchema(
            label = "Justificativa",
            controlType = FieldControlType.TEXTAREA,
            required = true,
            maxLength = 500,
            icon = "notes"
    )
    @Schema(
            description = "Razao da transicao; auditoria e compliance (texto curto).")
    private String justificativa;

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}

