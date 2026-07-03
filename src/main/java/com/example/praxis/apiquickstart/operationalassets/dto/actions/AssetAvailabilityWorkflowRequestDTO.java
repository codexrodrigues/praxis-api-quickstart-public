package com.example.praxis.apiquickstart.operationalassets.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "AssetAvailabilityWorkflowRequestDTO",
        description = "Comando de disponibilidade para enviar um ativo a manutencao ou devolve-lo ao uso operacional.")
public class AssetAvailabilityWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(description = "Justificativa de manutencao, inspecao ou liberacao que explica a mudanca de disponibilidade do ativo.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
