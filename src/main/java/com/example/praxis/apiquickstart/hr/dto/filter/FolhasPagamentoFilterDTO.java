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

/**
 * Filtro de folhas de pagamento orientado por competencia, colaborador e faixas monetarias.
 *
 * <p>Esse contrato foi mantido enxuto de proposito para exemplificar o caso mais
 * comum de backoffice: localizar a folha certa por ano/mes/funcionario e depois
 * restringir por data de pagamento ou valores financeiros.
 */
@Schema(
        name = "FolhasPagamentoFilterDTO",
        description = "Criterios de busca de folha por competencia, colaborador e montante (nao e o fecho/ contracheque em edicao). "
                + "Backoffice: localizar competencia/funcionario e refinar data ou valores; ver javadoc de classe. GenericFilter (demo).")
public class FolhasPagamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Ano de Referência", type = FieldDataType.NUMBER, order = 10, helpText = "Filtrar pelo ano de competência.", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ano civil da competencia; EQUAL (ex.: 2025) (demo).")
    private Integer ano;

    @UISchema(label = "Mês de Referência", type = FieldDataType.NUMBER, order = 20, helpText = "Filtrar pelo mês de competência.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Mes da competencia (1-12); EQUAL em conjunto com ano (demo).")
    private Integer mes;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar folhas de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Folha do colaborador; EQUAL (FK) — reduz resultado a uma pessoa (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Período de Pagamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar por intervalo de data de pagamento.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataPagamento")
    @Schema(
            description = "Janela de data efetiva de credito/liquidacao; BETWEEN (reconciliacao bancaria) (demo).")
    private List<LocalDate> dataPagamentoBetween;

    @UISchema(label = "Salário Bruto", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 50,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de salário bruto.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioBruto")
    @Schema(
            description = "Faixa de provento bruto consolidado; BETWEEN na moeda do backend (demo).")
    private List<BigDecimal> salarioBrutoBetween;

    @UISchema(label = "Total de Descontos", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 60,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de total de descontos.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalDescontos")
    @Schema(
            description = "Soma de descontos; BETWEEN para achar folhas muito oneradas (demo).")
    private List<BigDecimal> totalDescontosBetween;

    @UISchema(label = "Salário Líquido", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 70,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de salário líquido.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioLiquido")
    @Schema(
            description = "Faixa de liquido a depositar; BETWEEN (regra de teto, auditoria) (demo).")
    private List<BigDecimal> salarioLiquidoBetween;

    @UISchema(label = "Data de Pagamento (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80, helpText = "Buscar por data de pagamento exata.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataPagamento")
    @Schema(
            description = "Folhas com pagamento nesse dia civil exato; ON_DATE (criterio alternativo ao intervalo) (GenericFilter) (demo).")
    private LocalDate dataPagamentoOn;

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<LocalDate> getDataPagamentoBetween() { return dataPagamentoBetween; }
    public void setDataPagamentoBetween(List<LocalDate> dataPagamentoBetween) { this.dataPagamentoBetween = dataPagamentoBetween; }
    public List<BigDecimal> getSalarioBrutoBetween() { return salarioBrutoBetween; }
    public void setSalarioBrutoBetween(List<BigDecimal> salarioBrutoBetween) { this.salarioBrutoBetween = salarioBrutoBetween; }
    public List<BigDecimal> getTotalDescontosBetween() { return totalDescontosBetween; }
    public void setTotalDescontosBetween(List<BigDecimal> totalDescontosBetween) { this.totalDescontosBetween = totalDescontosBetween; }
    public List<BigDecimal> getSalarioLiquidoBetween() { return salarioLiquidoBetween; }
    public void setSalarioLiquidoBetween(List<BigDecimal> salarioLiquidoBetween) { this.salarioLiquidoBetween = salarioLiquidoBetween; }
    public LocalDate getDataPagamentoOn() { return dataPagamentoOn; }
    public void setDataPagamentoOn(LocalDate dataPagamentoOn) { this.dataPagamentoOn = dataPagamentoOn; }
}
