package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartBusinessDateRangeUiOptions;
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
                + "Apoia backoffice financeiro a localizar lotes, conciliacao de pagamento e desvios de valores por competencia.")
public class FolhasPagamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Ano de Referência", type = FieldDataType.NUMBER, order = 10, helpText = "Filtrar pelo ano de competência.", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ano civil da competencia da folha, usado com o mes para localizar o periodo remuneratorio contabil.")
    private Integer ano;

    @UISchema(label = "Mês de Referência", type = FieldDataType.NUMBER, order = 20, helpText = "Filtrar pelo mês de competência.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Mes da competencia remuneratoria, de 1 a 12, usado com o ano para identificar o ciclo de folha.")
    private Integer mes;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar folhas de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador cuja folha deve ser localizada para atendimento, conferencia individual ou conciliacao de pagamento.")
    private Integer funcionarioId;

    @UISchema(label = "Período de Pagamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40,
            helpText = "Buscar por intervalo de data de pagamento.", icon = "event",
            extraProperties = {
                    @ExtensionProperty(name = "shortcuts", value = QuickstartBusinessDateRangeUiOptions.PAYROLL_PAYMENT_SHORTCUTS_JSON),
                    @ExtensionProperty(name = "inlineQuickPresets", value = QuickstartBusinessDateRangeUiOptions.FOOTER_INLINE_PRESETS_JSON),
                    @ExtensionProperty(name = "inlineOverlay", value = QuickstartBusinessDateRangeUiOptions.EXPLICIT_INLINE_OVERLAY_JSON)
            })
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataPagamento")
    @Schema(
            description = "Intervalo de datas efetivas de credito ou liquidacao, usado para reconciliacao bancaria e controle de lote.")
    private List<LocalDate> dataPagamentoBetween;

    @UISchema(label = "Salário Bruto", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 50,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de salário bruto.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioBruto")
    @Schema(
            description = "Intervalo de valor bruto consolidado da folha antes de descontos, usado para analise de massa salarial.")
    private List<BigDecimal> salarioBrutoBetween;

    @UISchema(label = "Total de Descontos", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 60,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de total de descontos.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalDescontos")
    @Schema(
            description = "Intervalo do total de descontos aplicados, usado para localizar incidencias elevadas ou divergencias de calculo.")
    private List<BigDecimal> totalDescontosBetween;

    @UISchema(label = "Salário Líquido", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 70,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar por faixa de salário líquido.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioLiquido")
    @Schema(
            description = "Intervalo de valor liquido a depositar, usado para conferir teto, piso, conciliacao e impacto financeiro individual.")
    private List<BigDecimal> salarioLiquidoBetween;

    @UISchema(label = "Pago em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80, helpText = "Busca folhas pagas em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataPagamento")
    @Schema(
            description = "Data civil exata em que o pagamento foi ou sera liquidado, usada quando a consulta mira um lote diario especifico.")
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
