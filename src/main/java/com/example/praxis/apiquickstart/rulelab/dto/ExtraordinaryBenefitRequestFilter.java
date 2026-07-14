package com.example.praxis.apiquickstart.rulelab.dto;

import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitLifecycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "ExtraordinaryBenefitRequestFilter",
        description = "Criterios de consulta da fila persistida de beneficios extraordinarios por referencia, estado e janela de avaliacao.")
public class ExtraordinaryBenefitRequestFilter implements GenericFilterDTO {
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @UISchema(label = "Referencia", controlType = FieldControlType.INPUT, order = 10)
    @Schema(description = "Trecho da referencia externa usado para localizar o atendimento ou processo de origem.")
    private String requestReference;

    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @UISchema(label = "Estado", controlType = FieldControlType.SELECT, order = 20)
    @Schema(description = "Estado exato do lifecycle operacional que deve compor a fila consultada.")
    private ExtraordinaryBenefitLifecycleStatus lifecycleStatus;

    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "evaluatedAt")
    @UISchema(label = "Periodo de avaliacao", controlType = FieldControlType.DATE_RANGE, order = 30)
    @Schema(description = "Intervalo UTC em que a decisao ALLOW foi avaliada e persistida.")
    private List<Instant> evaluatedAtBetween;

    public String getRequestReference() { return requestReference; }
    public void setRequestReference(String requestReference) { this.requestReference = requestReference; }
    public ExtraordinaryBenefitLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(ExtraordinaryBenefitLifecycleStatus lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public List<Instant> getEvaluatedAtBetween() { return evaluatedAtBetween; }
    public void setEvaluatedAtBetween(List<Instant> evaluatedAtBetween) { this.evaluatedAtBetween = evaluatedAtBetween; }
}
