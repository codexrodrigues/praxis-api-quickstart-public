package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ProcurementSupplierWorkflowRequestDTO",
        description = "Comando de governanca para bloquear ou reabilitar fornecedor, preservando a justificativa operacional da decisao.")
public class ProcurementSupplierWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(description = "Justificativa de compliance, risco ou auditoria para explicar a mudanca de elegibilidade do fornecedor.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
