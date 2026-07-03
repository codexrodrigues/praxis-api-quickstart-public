package com.example.praxis.apiquickstart.riskintelligence.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ThreatTriageWorkflowRequestDTO",
        description = "Comando de triagem para alterar o ciclo operacional de uma ameaca monitorada.")
public class ThreatTriageWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(description = "Justificativa de inteligencia, captura, monitoramento ou resposta operacional.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
