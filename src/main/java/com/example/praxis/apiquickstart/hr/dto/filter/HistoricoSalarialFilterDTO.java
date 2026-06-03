package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "HistoricoSalarialFilterDTO",
        description = "Criterios de busca no historico de correcoes/ faixas salariais (nao e a linha de ajuste a submeter). "
                + "Uso: auditoria de reajustes; GenericFilter / POST /filter (demo).")
public class HistoricoSalarialFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar por colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Trilha de salario de um unico colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Faixa Salarial", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 20,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por faixa salarial no histórico.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salario")
    @Schema(
            description = "Faixa de remuneracao no periodo historico; BETWEEN (reajuste, plano de carreira) (demo).")
    private List<BigDecimal> salarioBetween;

    @UISchema(label = "Início de Vigência", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 30, helpText = "Buscar por data de início de vigência.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataInicio")
    @Schema(
            description = "Inicio de vigencia do salario; BETWEEN (janela de validade) (demo).")
    private List<LocalDate> dataInicioBetween;

    @UISchema(label = "Fim de Vigência", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar por data de fim de vigência.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataFim")
    @Schema(
            description = "Fim de vigencia; BETWEEN (fim de contrato, migracao) (demo).")
    private List<LocalDate> dataFimBetween;

    @UISchema(label = "Motivo", controlType = FieldControlType.INPUT, maxLength = 2000, order = 50, helpText = "Filtrar por motivo do reajuste ou promoção.", icon = "notes")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Motivo textual (promocao, reajuste coletivo, correcao); LIKE (demo).")
    private String motivo;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<BigDecimal> getSalarioBetween() { return salarioBetween; }
    public void setSalarioBetween(List<BigDecimal> salarioBetween) { this.salarioBetween = salarioBetween; }
    public List<LocalDate> getDataInicioBetween() { return dataInicioBetween; }
    public void setDataInicioBetween(List<LocalDate> dataInicioBetween) { this.dataInicioBetween = dataInicioBetween; }
    public List<LocalDate> getDataFimBetween() { return dataFimBetween; }
    public void setDataFimBetween(List<LocalDate> dataFimBetween) { this.dataFimBetween = dataFimBetween; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
