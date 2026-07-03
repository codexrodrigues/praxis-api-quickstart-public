package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ProcurementContractWorkflowRequestDTO",
        description = "Comando de ciclo de vida para assinar, suspender ou reativar contrato de fornecimento.")
public class ProcurementContractWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(description = "Justificativa de negocio, compliance ou auditoria para explicar a decisao sobre o contrato.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
