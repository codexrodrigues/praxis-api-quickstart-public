package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "AbsenceCoverageWorkflowRequestDTO",
        description = "Comando para registrar a decisao operacional de cobertura de uma ausencia de colaborador. "
                + "A action nao substitui o calendario de ferias/afastamentos; ela explicita quem cobriu a janela e por que a cobertura foi aceita.")
public class AbsenceCoverageWorkflowRequestDTO {

    @NotBlank
    @Size(max = 500)
    @UISchema(label = "Plano de cobertura", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 10, helpText = "Como a ausencia sera coberta operacionalmente.", icon = "assignment_turned_in")
    @Schema(
            description = "Plano operacional para cobrir a indisponibilidade, como substituicao, redistribuicao de escala ou aceite de risco controlado.",
            example = "Cobertura pela equipe B durante a primeira semana; chamados criticos ficam com lideranca de plantao.")
    private String planoCobertura;

    @UISchema(label = "Substituto", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Colaborador que cobre a ausencia, quando houver substituto nominal.", icon = "badge")
    @Schema(
            description = "Funcionario substituto que cobre a ausencia. Opcional porque alguns planos redistribuem a cobertura por equipe.",
            example = "12")
    private Integer substitutoFuncionarioId;

    @Size(max = 500)
    @UISchema(label = "Justificativa", controlType = FieldControlType.TEXTAREA, maxLength = 500, order = 30, helpText = "Motivo da decisao de cobertura.", icon = "notes")
    @Schema(
            description = "Justificativa da decisao de cobertura, preservada para auditoria de RH, capacidade operacional e revisao posterior.",
            example = "A ausencia cruza fechamento de folha e duas missoes, exigindo cobertura formal.")
    private String justificativa;

    public String getPlanoCobertura() {
        return planoCobertura;
    }

    public void setPlanoCobertura(String planoCobertura) {
        this.planoCobertura = planoCobertura;
    }

    public Integer getSubstitutoFuncionarioId() {
        return substitutoFuncionarioId;
    }

    public void setSubstitutoFuncionarioId(Integer substitutoFuncionarioId) {
        this.substitutoFuncionarioId = substitutoFuncionarioId;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
