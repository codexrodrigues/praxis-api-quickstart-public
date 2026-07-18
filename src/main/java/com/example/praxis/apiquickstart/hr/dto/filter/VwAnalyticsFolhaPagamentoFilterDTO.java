package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopedFilter;
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
 * Filtro analitico da view de folha.
 *
 * <p>Combina identidade organizacional da pessoa, contexto operacional herdado
 * do dominio de herois e buckets numericos usados em analises de remuneracao.
 */
@Schema(
        name = "VwAnalyticsFolhaPagamentoFilterDTO",
        description = "Criterios de busca sobre a projecao VwAnalyticsFolhaPagamento (vista, nao entidade a persistir). "
                + "Filtra linhas agregadas de folha com contexto de colaborador, organograma, operacoes e buckets de remuneracao, sem substituir o contrato transacional de FolhaPagamento.")
public class VwAnalyticsFolhaPagamentoFilterDTO implements GenericFilterDTO, HrDepartmentScopedFilter {
    @UISchema(label = "Mostrar colaboradores", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 15,
            multiple = true,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Inclui na análise apenas os colaboradores selecionados.", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "funcionarioId")
    @Schema(
            description = "Conjunto de colaboradores que devem compor a analise de folha, usando os identificadores desnormalizados da vista.")
    private List<Integer> funcionarioIdsIn;

    @UISchema(label = "Universo", controlType = FieldControlType.ASYNC_SELECT, maxLength = 120, order = 40,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/universo/options/filter", helpText = "Filtrar por universo ou franquia de origem.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Agrupador ficcional da identidade; EQUAL via option-source da view (ver endpoint).")
    private String universo;

    // Alias tecnico usado para demonstrar cascata canonica quando o campo UI difere da chave de filtro.
    @UISchema(label = "Universo contextual", controlType = FieldControlType.INPUT, maxLength = 120, order = 41, formHidden = true, helpText = "Campo auxiliar oculto para reaproveitar o mesmo valor de universo no contrato de filtro.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "universo")
    @Schema(
            description = "Valor contextual de universo reaproveitado pelo contrato de filtro quando a UI precisa manter a mesma semantica em campo auxiliar.")
    private String universoContexto;

    @UISchema(label = "Exposição pública", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 50, helpText = "Filtra por colaboradores com identidade pública ou protegida.", icon = "visibility")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Indicador que separa colaboradores com exposicao publica de identidades protegidas nas analises de folha.")
    private Boolean exposicaoPublica;

    @UISchema(label = "Ano", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 60, helpText = "Analisar por intervalo de anos.", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ano")
    @Schema(
            description = "Intervalo de anos civis considerado na agregacao da folha.")
    private List<Integer> anoBetween;

    @UISchema(label = "Mês", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70, helpText = "Analisa a folha por intervalo de meses.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "mes")
    @Schema(
            description = "Intervalo de meses dentro dos anos analisados, usando valores de 1 a 12.")
    private List<Integer> mesBetween;

    @UISchema(label = "Competência", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 80, helpText = "Busca por janela de competência da folha.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "competencia")
    @Schema(
            description = "Janela de competencia da folha, usada como data ancora do periodo remuneratorio.")
    private List<LocalDate> competenciaBetween;

    @UISchema(label = "Data de pagamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 90, helpText = "Busca por janela de depósito ou liquidação da folha.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataPagamento")
    @Schema(
            description = "Janela de credito ou liquidacao da folha, usada para cruzar remuneracao com tesouraria.")
    private List<LocalDate> dataPagamentoBetween;

    @UISchema(label = "Cargo", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 100,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS, helpText = "Filtrar análise por cargo ocupado.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "cargoId")
    @Schema(
            description = "Cargo ocupado pelo colaborador na linha analitica, usando o identificador desnormalizado da vista.")
    private Integer cargoId;

    @UISchema(label = "Departamento", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 110,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS, helpText = "Filtrar pela lotação efetiva na competência.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "departamentoId")
    @Schema(
            description = "Departamento efetivo no primeiro dia da competência, sem fallback para o cadastro atual do colaborador.")
    private Integer departamentoId;

    @UISchema(label = "Departamentos", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 115,
            multiple = true, valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS, formHidden = true, icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "departamentoId")
    @Schema(description = "Conjunto de departamentos efetivos. O backend intersecta este filtro com o escopo organizacional resolvido para o principal.")
    private List<Integer> departamentoIdsIn;

    @UISchema(label = "Equipe", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 120,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS, helpText = "Filtrar análise por equipe tática.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipeId")
    @Schema(
            description = "Equipe tatica de Operacoes relacionada ao colaborador no contexto analitico da folha.")
    private Integer equipeId;

    @UISchema(label = "Base", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 130,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES_BASE_LOOKUP_OPTIONS, helpText = "Filtrar análise por base operacional.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "baseId")
    @Schema(
            description = "Base operacional relacionada ao colaborador no contexto analitico da folha.")
    private Integer baseId;

    // Campos textuais ocultos no formulario principal, preservados para busca livre e compatibilidade com listagens.
    @UISchema(label = "Nome do cargo", controlType = FieldControlType.INPUT, maxLength = 120, order = 140, formHidden = true, helpText = "Campo auxiliar oculto para busca textual pelo nome do cargo.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome de cargo exibido na linha analitica, usado em busca textual quando o identificador do cargo nao esta disponivel.")
    private String cargo;

    @UISchema(label = "Nome do departamento", controlType = FieldControlType.INPUT, maxLength = 120, order = 150, formHidden = true, helpText = "Campo auxiliar oculto para busca textual pelo nome do departamento.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome de departamento desnormalizado exibido na linha analitica.")
    private String departamento;

    @UISchema(label = "Nome da equipe", controlType = FieldControlType.INPUT, maxLength = 120, order = 160, formHidden = true, helpText = "Campo auxiliar oculto para busca textual pelo nome da equipe.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome de equipe exibido na linha analitica, usado quando o identificador da equipe nao esta disponivel.")
    private String equipe;

    @UISchema(label = "Nome da base", controlType = FieldControlType.INPUT, maxLength = 120, order = 170, formHidden = true, helpText = "Campo auxiliar oculto para busca textual pelo nome da base.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome de base operacional exibido na linha analitica.")
    private String base;

    @UISchema(label = "Perfil da folha", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 180,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/payrollProfile/options/filter", helpText = "Filtrar por perfil analítico da folha.", icon = "receipt_long")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Segmentacao de politica de remuneracao usada para agrupar perfis de folha e beneficios.")
    private String payrollProfile;

    @UISchema(label = "Composição da folha", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 190,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/composicaoFolha/options/filter", helpText = "Filtrar pela composição de receitas e despesas.", icon = "receipt_long")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classificacao qualitativa da composicao da folha, como predominancia de proventos ou descontos.")
    private String composicaoFolha;

    @UISchema(label = "Faixa de salário bruto", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 200,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaSalarioBruto/options/filter", helpText = "Agrupar análise pela faixa de salário bruto.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Faixa rotulada de salario bruto usada em histogramas e comparacoes de remuneracao.")
    private String faixaSalarioBruto;

    @UISchema(label = "Faixa de salário líquido", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 210,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaSalarioLiquido/options/filter", helpText = "Agrupar análise pela faixa de salário líquido.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Faixa rotulada de salario liquido usada para segmentar renda disponivel.")
    private String faixaSalarioLiquido;

    @UISchema(label = "Faixa % Desconto", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 220,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaPctDesconto/options/filter", helpText = "Agrupar análise pelo percentual retido.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Faixa rotulada de intensidade de desconto agregada sobre a remuneracao.")
    private String faixaPctDesconto;

    @UISchema(label = "Salário bruto", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 230,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de valores brutos.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioBruto")
    @Schema(
            description = "Faixa numerica de salario bruto em moeda, antes de descontos.")
    private List<BigDecimal> salarioBrutoBetween;

    @UISchema(label = "Total de descontos", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 240,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de descontos aplicados.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalDescontos")
    @Schema(
            description = "Faixa de soma de descontos aplicados na folha.")
    private List<BigDecimal> totalDescontosBetween;

    @UISchema(label = "Salário líquido", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 250,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de valores líquidos.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioLiquido")
    @Schema(
            description = "Faixa numerica de salario liquido depois dos descontos.")
    private List<BigDecimal> salarioLiquidoBetween;

    @UISchema(label = "% Desconto", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 260,
            numericFormat = NumericFormat.PERCENT, numericStep = "0.01", helpText = "Filtrar por proporção de descontos.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "pctDesconto")
    @Schema(
            description = "Intervalo de percentual de desconto agregado, usado para detectar folhas mais oneradas.")
    private List<BigDecimal> pctDescontoBetween;

    @UISchema(label = "Valores adicionais", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 270,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de ganhos adicionais.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "valorAdicionais")
    @Schema(
            description = "Faixa de valores adicionais agregados, representando proventos extras no periodo.")
    private List<BigDecimal> valorAdicionaisBetween;

    @UISchema(label = "Quantidade de eventos", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 280, helpText = "Filtra por quantidade de eventos que compõem a folha.", icon = "event_note")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdEventos")
    @Schema(
            description = "Faixa de quantidade de eventos ou rubricas que compoem a folha, usada como sinal de complexidade do contracheque.")
    private List<Long> qtdEventosBetween;

    public List<Integer> getFuncionarioIdsIn() { return funcionarioIdsIn; }
    public void setFuncionarioIdsIn(List<Integer> funcionarioIdsIn) { this.funcionarioIdsIn = funcionarioIdsIn; }
    public String getUniverso() { return universo; }
    public void setUniverso(String universo) { this.universo = universo; }
    public String getUniversoContexto() { return universoContexto; }
    public void setUniversoContexto(String universoContexto) { this.universoContexto = universoContexto; }
    public Boolean getExposicaoPublica() { return exposicaoPublica; }
    public void setExposicaoPublica(Boolean exposicaoPublica) { this.exposicaoPublica = exposicaoPublica; }
    public List<Integer> getAnoBetween() { return anoBetween; }
    public void setAnoBetween(List<Integer> anoBetween) { this.anoBetween = anoBetween; }
    public List<Integer> getMesBetween() { return mesBetween; }
    public void setMesBetween(List<Integer> mesBetween) { this.mesBetween = mesBetween; }
    public List<LocalDate> getCompetenciaBetween() { return competenciaBetween; }
    public void setCompetenciaBetween(List<LocalDate> competenciaBetween) { this.competenciaBetween = competenciaBetween; }
    public List<LocalDate> getDataPagamentoBetween() { return dataPagamentoBetween; }
    public void setDataPagamentoBetween(List<LocalDate> dataPagamentoBetween) { this.dataPagamentoBetween = dataPagamentoBetween; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public Integer getCargoId() { return cargoId; }
    public void setCargoId(Integer cargoId) { this.cargoId = cargoId; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public Integer getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Integer departamentoId) { this.departamentoId = departamentoId; }
    public List<Integer> getDepartamentoIdsIn() { return departamentoIdsIn; }
    public void setDepartamentoIdsIn(List<Integer> departamentoIdsIn) { this.departamentoIdsIn = departamentoIdsIn; }
    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }
    public Integer getEquipeId() { return equipeId; }
    public void setEquipeId(Integer equipeId) { this.equipeId = equipeId; }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public Integer getBaseId() { return baseId; }
    public void setBaseId(Integer baseId) { this.baseId = baseId; }
    public String getPayrollProfile() { return payrollProfile; }
    public void setPayrollProfile(String payrollProfile) { this.payrollProfile = payrollProfile; }
    public String getComposicaoFolha() { return composicaoFolha; }
    public void setComposicaoFolha(String composicaoFolha) { this.composicaoFolha = composicaoFolha; }
    public String getFaixaSalarioBruto() { return faixaSalarioBruto; }
    public void setFaixaSalarioBruto(String faixaSalarioBruto) { this.faixaSalarioBruto = faixaSalarioBruto; }
    public String getFaixaSalarioLiquido() { return faixaSalarioLiquido; }
    public void setFaixaSalarioLiquido(String faixaSalarioLiquido) { this.faixaSalarioLiquido = faixaSalarioLiquido; }
    public String getFaixaPctDesconto() { return faixaPctDesconto; }
    public void setFaixaPctDesconto(String faixaPctDesconto) { this.faixaPctDesconto = faixaPctDesconto; }
    public List<BigDecimal> getSalarioBrutoBetween() { return salarioBrutoBetween; }
    public void setSalarioBrutoBetween(List<BigDecimal> salarioBrutoBetween) { this.salarioBrutoBetween = salarioBrutoBetween; }
    public List<BigDecimal> getTotalDescontosBetween() { return totalDescontosBetween; }
    public void setTotalDescontosBetween(List<BigDecimal> totalDescontosBetween) { this.totalDescontosBetween = totalDescontosBetween; }
    public List<BigDecimal> getSalarioLiquidoBetween() { return salarioLiquidoBetween; }
    public void setSalarioLiquidoBetween(List<BigDecimal> salarioLiquidoBetween) { this.salarioLiquidoBetween = salarioLiquidoBetween; }
    public List<BigDecimal> getPctDescontoBetween() { return pctDescontoBetween; }
    public void setPctDescontoBetween(List<BigDecimal> pctDescontoBetween) { this.pctDescontoBetween = pctDescontoBetween; }
    public List<BigDecimal> getValorAdicionaisBetween() { return valorAdicionaisBetween; }
    public void setValorAdicionaisBetween(List<BigDecimal> valorAdicionaisBetween) { this.valorAdicionaisBetween = valorAdicionaisBetween; }
    public List<Long> getQtdEventosBetween() { return qtdEventosBetween; }
    public void setQtdEventosBetween(List<Long> qtdEventosBetween) { this.qtdEventosBetween = qtdEventosBetween; }
}
