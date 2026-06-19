package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * Payload minimo para workflow actions de item da folha de pagamento.
 *
 * <p>O quickstart mantem este DTO propositalmente enxuto para mostrar que uma action pode exigir
 * apenas a justificativa operacional da transicao, sem arrastar o schema completo do recurso.</p>
 */
@Schema(
        name = "FolhaPagamentoWorkflowRequestDTO",
        description = "Comando de workflow para decisao sobre uma folha de pagamento, como aprovar eventos pendentes ou marcar liquidacao. "
                + "A justificativa registra o motivo operacional da decisao sem enviar novamente o recurso completo.")
public class FolhaPagamentoWorkflowRequestDTO {

    @Size(max = 500)
    @UISchema(label = "Justification", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, helpText = "Motivo da aprovação ou rejeição da folha.", icon = "notes")
    @Schema(
            description = "Texto operacional explicando a decisao; opcional no schema, mas a politica de aprovacao pode exigi-la; maximo 500 caracteres.",
            example = "Ciente do impacto de folha, aprovo processamento com competencia 04-2025.")
    private String justificativa;

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
