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
 * Filtro analitico da view de folha.
 *
 * <p>Combina identidade organizacional da pessoa, contexto operacional herdado
 * do dominio de herois e buckets numericos usados em analises de remuneracao.
 */
@Schema(
        name = "VwAnalyticsFolhaPagamentoFilterDTO",
        description = "Criterios de busca sobre a projecao VwAnalyticsFolhaPagamento (vista, nao entidade a persistir). "
                + "Filtra linhas agregadas de analise de folha com contexto de heroi, organograma e buckets; nao e o mesmo contrato que FolhaPagamentoFilterDTO. "
                + "GenericFilter / POST /filter; ver javadoc de classe. (demo RH).")
public class VwAnalyticsFolhaPagamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Funcionarios (Incluir)", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, order = 15,
            multiple = true,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Selecionar múltiplos colaboradores para análise.", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "funcionarioId")
    @Schema(
            description = "Conjunto de colaboradores a incluir (multiplos id); operacao IN na coluna desnormalizada funcionarioId da vista (demo).")
    private List<Integer> funcionarioIdsIn;

    @UISchema(label = "Universo", controlType = FieldControlType.ASYNC_SELECT, maxLength = 120, order = 40,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/universo/options/filter", helpText = "Filtrar por universo ou franquia de origem.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Agrupador ficcional da identidade; EQUAL via option-source da view (ver endpoint).")
    private String universo;

    // Alias tecnico usado para demonstrar cascata canonica quando o campo UI difere da chave de filtro.
    @UISchema(label = "Universo (Contexto)", controlType = FieldControlType.INPUT, maxLength = 120, order = 41, formHidden = true, helpText = "Alias contextual de universo (uso técnico).", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "universo")
    @Schema(
            description = "Alias de filtro alinhado a coluna universo; uso para contexto/ URL sem duplicar semantica (demo).")
    private String universoContexto;

    @UISchema(label = "Exposicao Publica", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 50, helpText = "Filtrar por permissão de exposição do herói.", icon = "visibility")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Recorte alter ego publico vs. reservado; EQUAL boolean (demo).")
    private Boolean exposicaoPublica;

    @UISchema(label = "Ano", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 60, helpText = "Analisar por intervalo de anos.", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ano")
    @Schema(
            description = "Intervalo de ano civil; BETWEEN na coluna agregada ano (ex.: 2024-2025) (demo).")
    private List<Integer> anoBetween;

    @UISchema(label = "Mes", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70, helpText = "Analisar por intervalo de meses.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "mes")
    @Schema(
            description = "Intervalo de mes (1-12) dentro do(s) ano(s) considerados; BETWEEN (demo).")
    private List<Integer> mesBetween;

    @UISchema(label = "Competencia", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 80, helpText = "Buscar por janela de competência da folha.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "competencia")
    @Schema(
            description = "Janela de competencia (data-ancora da folha); BETWEEN LocalDate (demo).")
    private List<LocalDate> competenciaBetween;

    @UISchema(label = "Data Pagamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 90, helpText = "Buscar por janela de depósito liquidado.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataPagamento")
    @Schema(
            description = "Janela de credito/liquidacao; BETWEEN; cruza com tesouraria (demo).")
    private List<LocalDate> dataPagamentoBetween;

    @UISchema(label = "Cargo", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 100,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.CARGOS + "/options/filter", helpText = "Filtrar análise por cargo ocupado.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "cargoId")
    @Schema(
            description = "Criterio: cargo por id; EQUAL — filtra a vista por cargoId desnormalizado (nao o texto de cargo) (demo).")
    private Integer cargoId;

    @UISchema(label = "Departamento", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 110,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS + "/options/filter", helpText = "Filtrar análise por departamento.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "departamentoId")
    @Schema(
            description = "Criterio: departamento por id; EQUAL (demo).")
    private Integer departamentoId;

    @UISchema(label = "Equipe", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 120,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES + "/options/filter", helpText = "Filtrar análise por equipe tática.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipeId")
    @Schema(
            description = "Criterio: equipa tatica (Operacoes) por id; EQUAL (demo).")
    private Integer equipeId;

    @UISchema(label = "Base", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 130,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES + "/options/filter", helpText = "Filtrar análise por base operacional.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "baseId")
    @Schema(
            description = "Criterio: base operacional por id; EQUAL (demo).")
    private Integer baseId;

    // Campos textuais ocultos no formulario principal, preservados para busca livre e compatibilidade com listagens.
    @UISchema(label = "Cargo (Texto)", controlType = FieldControlType.INPUT, maxLength = 120, order = 140, formHidden = true, helpText = "Busca livre pelo nome do cargo.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em rotulo de cargo exibido na linha; LIKE; alternativa ad hoc ao select por id (form oculto) (demo).")
    private String cargo;

    @UISchema(label = "Departamento (Texto)", controlType = FieldControlType.INPUT, maxLength = 120, order = 150, formHidden = true, helpText = "Busca livre pelo nome do departamento.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em nome de departamento desnormalizado; LIKE (demo).")
    private String departamento;

    @UISchema(label = "Equipe (Texto)", controlType = FieldControlType.INPUT, maxLength = 120, order = 160, formHidden = true, helpText = "Busca livre pelo nome da equipe.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em nome de equipe; LIKE; quando id nao conhecido (demo).")
    private String equipe;

    @UISchema(label = "Base (Texto)", controlType = FieldControlType.INPUT, maxLength = 120, order = 170, formHidden = true, helpText = "Busca livre pelo nome da base.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em nome de base; LIKE (demo).")
    private String base;

    @UISchema(label = "Perfil Folha", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 180,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/payrollProfile/options/filter", helpText = "Filtrar por perfil analítico da folha.", icon = "receipt_long")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Segmentacao de politica de remuneracao (classe de perfil); EQUAL via option-source (demo).")
    private String payrollProfile;

    @UISchema(label = "Composicao Folha", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 190,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/composicaoFolha/options/filter", helpText = "Filtrar pela composição de receitas e despesas.", icon = "receipt_long")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Composicao qualitativa (ex.: carga de provento vs. desconto); EQUAL; bucket derivado (demo).")
    private String composicaoFolha;

    @UISchema(label = "Faixa Salario Bruto", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 200,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaSalarioBruto/options/filter", helpText = "Agrupar análise pela faixa de salário bruto.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Histograma de bruto; EQUAL por rotulo de faixa (option-source) (demo).")
    private String faixaSalarioBruto;

    @UISchema(label = "Faixa Salario Liquido", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 210,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaSalarioLiquido/options/filter", helpText = "Agrupar análise pela faixa de salário líquido.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Faixa de liquido; EQUAL (seguranca de renda) (demo).")
    private String faixaSalarioLiquido;

    @UISchema(label = "Faixa % Desconto", controlType = FieldControlType.ASYNC_SELECT, maxLength = 80, order = 220,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/option-sources/faixaPctDesconto/options/filter", helpText = "Agrupar análise pelo percentual retido.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Intensidade de desconto agregada; EQUAL por faixa (demo).")
    private String faixaPctDesconto;

    @UISchema(label = "Salario Bruto", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 230,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de valores brutos.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioBruto")
    @Schema(
            description = "Faixa numerica de bruto; BETWEEN (moeda) (demo).")
    private List<BigDecimal> salarioBrutoBetween;

    @UISchema(label = "Total Descontos", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 240,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de descontos aplicados.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalDescontos")
    @Schema(
            description = "Faixa de soma de descontos; BETWEEN (demo).")
    private List<BigDecimal> totalDescontosBetween;

    @UISchema(label = "Salario Liquido", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 250,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de valores líquidos.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioLiquido")
    @Schema(
            description = "Faixa de liquido; BETWEEN; cruza com faixa por rotulo (acima) (demo).")
    private List<BigDecimal> salarioLiquidoBetween;

    @UISchema(label = "% Desconto", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 260,
            numericFormat = NumericFormat.PERCENT, numericStep = "0.01", helpText = "Filtrar por proporção de descontos.", icon = "money_off")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "pctDesconto")
    @Schema(
            description = "Intervalo de percentual de desconto; BETWEEN (0-1 ou 0-100 conforme apresentacao da vista) (demo).")
    private List<BigDecimal> pctDescontoBetween;

    @UISchema(label = "Valor Adicionais", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 270,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Filtrar por intervalo de ganhos adicionais.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "valorAdicionais")
    @Schema(
            description = "Intervalo de adicionais agregados; BETWEEN (provento extra) (demo).")
    private List<BigDecimal> valorAdicionaisBetween;

    @UISchema(label = "Qtd Eventos", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 280, helpText = "Filtrar por quantidade de eventos na folha.", icon = "event_note")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdEventos")
    @Schema(
            description = "Contagem de rubricas na folha; BETWEEN para complexidade de contracheque (demo).")
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
