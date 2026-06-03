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
        description = "Corpo minimo para transicoes de workflow sobre item de folha (ex.: aprovar, rejeitar, devolver). "
                + "O quickstart nao exige o DTO completo do recurso; a justificativa e o suporte a auditoria e a governanca de aprovacao (demo).")
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
