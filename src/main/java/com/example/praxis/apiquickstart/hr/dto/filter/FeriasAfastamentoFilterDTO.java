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
                + "Uteis a escala e cobertura; GenericFilter / POST /filter (demo).")
public class FeriasAfastamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Tipo de Afastamento", controlType = FieldControlType.INPUT, maxLength = 100, order = 10, helpText = "Filtrar por tipo de ausência.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Classe de ausencia (ferias, afastamento medico, treino); string catalogada na UI; LIKE.")
    private String tipo;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar ausências de um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Apenas historico do colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Período Inicial", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 30, helpText = "Filtrar por data de início no intervalo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataInicio")
    @Schema(
            description = "Inicio de periodo (range); BETWEEN; cruza com data fim para achar conflitos de cobertura (demo).")
    private List<LocalDate> dataInicioBetween;

    @UISchema(label = "Período Final", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Filtrar por data de término no intervalo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataFim")
    @Schema(
            description = "Fim de periodo; BETWEEN; usar com inicio para janela completa (demo).")
    private List<LocalDate> dataFimBetween;

    @UISchema(label = "Observações", controlType = FieldControlType.INPUT, maxLength = 2000, order = 50, helpText = "Filtrar por notas ou justificativas.", icon = "notes")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Notas livres (substituicao, aprovador); LIKE para rastreio de texto (demo).")
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
