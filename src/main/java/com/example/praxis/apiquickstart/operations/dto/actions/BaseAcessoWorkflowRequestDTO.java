package com.example.praxis.apiquickstart.operations.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "BaseAcessoWorkflowRequestDTO",
        description = "Comando de elegibilidade para ativar ou desativar o acesso de uma pessoa a uma base operacional. "
                + "A justificativa registra a decisao de credenciamento quando a politica exigir rastro.")
public class BaseAcessoWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(
            description = "Motivo da ativacao ou desativacao da credencial de base; recomendado para revisao de seguranca e auditoria de acesso.")
    private String justificativa;

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}

