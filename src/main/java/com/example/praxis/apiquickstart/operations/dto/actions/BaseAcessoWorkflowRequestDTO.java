package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "BaseAcessoWorkflowRequestDTO",
        description = "Requisicao de transicao de workflow de acesso a base: justificativa opcional (ex. deferimento em 2o nivel).")
public class BaseAcessoWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(
            description = "Razao da decisao; recomendada quando a politica exige rastro.")
    private String justificativa;

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}

