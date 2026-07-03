package com.example.praxis.apiquickstart.procurement.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ProcurementPurchaseOrderWorkflowRequestDTO",
        description = "Comando de ciclo de vida para aprovar, cancelar ou receber pedido de compra.")
public class ProcurementPurchaseOrderWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, icon = "notes")
    @Schema(description = "Justificativa de negocio, compliance ou auditoria para explicar a decisao sobre o pedido.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
