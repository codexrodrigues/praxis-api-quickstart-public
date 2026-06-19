package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "FeriasAfastamentoFilterDTO",
        description = "Criterios de busca de ausencias (ferias, licenca, missao, etc.); nao e o lancamento a editar. "
                + "Apoia analise de disponibilidade, cobertura operacional, conflitos de escala e historico de afastamentos.")
public class FeriasAfastamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Tipo de Afastamento", controlType = FieldControlType.INPUT, maxLength = 100, order = 10, helpText = "Filtrar por tipo de ausência.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Classe de ausencia (ferias, afastamento medico, treino); string catalogada na UI; LIKE.")
    private String tipo;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar ausências de um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador cujo historico de ausencias deve ser consultado para disponibilidade, aprovacao ou auditoria.")
    private Integer funcionarioId;

    @UISchema(label = "Período Inicial", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 30, helpText = "Filtrar por data de início no intervalo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataInicio")
    @Schema(
            description = "Intervalo de datas de inicio da ausencia, usado para identificar quando a indisponibilidade comeca e cruzar conflitos de escala.")
    private List<LocalDate> dataInicioBetween;

    @UISchema(label = "Período Final", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Filtrar por data de término no intervalo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataFim")
    @Schema(
            description = "Intervalo de datas de fim da ausencia, usado para identificar retorno previsto e recomposicao de cobertura.")
    private List<LocalDate> dataFimBetween;

    @UISchema(label = "Observações", controlType = FieldControlType.INPUT, maxLength = 2000, order = 50, helpText = "Filtrar por notas ou justificativas.", icon = "notes")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Observacoes de negocio sobre a ausencia, como justificativa, substituicao planejada, aprovador ou restricao operacional.")
    private String observacoes;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<LocalDate> getDataInicioBetween() { return dataInicioBetween; }
    public void setDataInicioBetween(List<LocalDate> dataInicioBetween) { this.dataInicioBetween = dataInicioBetween; }
    public List<LocalDate> getDataFimBetween() { return dataFimBetween; }
    public void setDataFimBetween(List<LocalDate> dataFimBetween) { this.dataFimBetween = dataFimBetween; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
