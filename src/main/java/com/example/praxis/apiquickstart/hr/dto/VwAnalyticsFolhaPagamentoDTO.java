package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.LocalDate;

@UISchema(label = "Analytics da Folha de Pagamento", readOnly = true, icon = "analytics")
@Schema(
        name = "VwAnalyticsFolhaPagamentoDTO",
        description = "Linha de vista so-leitura: folha de pagamento cruzada com contexto de heroi (civil, codinome, universo), organograma, operacoes e faixas analiticas. "
                + "Nao e o DocumentoFolha editavel; e projecao para BI, graficos e LLM. Colunas de valor e percentagem refletem regras de calculo do snapshot (demo).")
public class VwAnalyticsFolhaPagamentoDTO {
    @Schema(
            description = "Chave da folha de pagamento (cabecalho) na origem; liga a competencia e eventos de folha subjacentes.",
            example = "9")
    @UISchema(label = "Cód. Folha", helpText = "Identificador interno da folha de pagamento.", formHidden = true, icon = "receipt_long")
    private Integer folhaPagamentoId;
    @Schema(
            description = "Colaborador dono desta linha analitica; usado com filtros e drill-down para o cadastro de Funcionario.",
            example = "3")
    @UISchema(label = "Cód. Colaborador", helpText = "Identificador do colaborador dono desta folha.", formHidden = true, icon = "badge")
    private Integer funcionarioId;

    @UISchema(label = "Nome Completo", helpText = "Nome do colaborador.", icon = "badge")
    @Schema(description = "Nome civil na competencia; desnormalizado para tabelas de massa salarial.")
    private String nomeCompleto;

    @UISchema(label = "Codinome", helpText = "Nome público do herói (se houver).", icon = "theater_comedy")
    @Schema(description = "Identidade de missao, quando houver, para cruzar com operacoes e marketing.")
    private String codinome;

    @UISchema(label = "Universo", helpText = "Universo narrativo de origem.", icon = "public")
    @Schema(description = "Agrupamento ficcional ou editorial (catalogo) herdado do perfil; segmenta relatorios.")
    private String universo;

    @UISchema(label = "Exposição Pública", type = FieldDataType.BOOLEAN, helpText = "Indica se há permissão de exposição pública.", icon = "badge")
    @Schema(
            description = "Indicador de exposicao do alter ego; afeta regras de visibilidade em analytics publicos (demo).",
            example = "true")
    private Boolean exposicaoPublica;

    @UISchema(label = "Cargo", helpText = "Cargo atual no momento da folha.", icon = "work")
    @Schema(description = "Descricao de cargo exibida na linha; texto da vista, nao FK de Cargo.")
    private String cargo;

    @UISchema(label = "Departamento", helpText = "Departamento alocado.", icon = "apartment")
    @Schema(description = "Celula organica; desnormalizado para corte de custo e headcount.")
    private String departamento;

    @UISchema(label = "Equipe", helpText = "Equipe tática do herói.", icon = "groups")
    @Schema(description = "Equipe tatica (Operacoes) associada; label na vista para comparar alocacoes e folha.")
    private String equipe;

    @UISchema(label = "Base", helpText = "Base operacional associada.", icon = "location_on")
    @Schema(description = "Base de operacoes; contexto logístico e de risco, nao endereco pessoal.")
    private String base;

    @UISchema(label = "Ano", type = FieldDataType.NUMBER, helpText = "Ano da competência da folha.", icon = "work")
    @Schema(
            description = "Ano de referencia do periodo de folha; junto com mes define a competencia civil.",
            example = "2025")
    private Integer ano;

    @UISchema(label = "Mês", type = FieldDataType.NUMBER, helpText = "Mês da competência da folha.", icon = "calendar_month")
    @Schema(
            description = "Mes civil da competencia (1-12).",
            example = "4")
    private Integer mes;

    @UISchema(label = "Competência", type = FieldDataType.DATE, helpText = "Data representativa da competência.", icon = "event")
    @Schema(
            description = "Data ancora de competencia (ex.: primeiro dia do mes) conforme a query; nao e necessariamente o dia de pagamento.",
            example = "2025-04-01")
    private LocalDate competencia;

    @UISchema(label = "Data Pagamento", type = FieldDataType.DATE, helpText = "Data de depósito liquidado.", icon = "event_available")
    @Schema(
            description = "Data efetiva de credito/liquidacao planejada para a linha; alinha com agendamento de folha (demo).",
            example = "2025-04-30")
    private LocalDate dataPagamento;

    @UISchema(label = "Salário Bruto", type = FieldDataType.NUMBER, helpText = "Total de ganhos brutos.", icon = "payments")
    @Schema(
            description = "Total bruto antes de descontos obrigatorios e eventos; base para faixas e percentuais exibidos na mesma linha.",
            example = "12000.00")
    private BigDecimal salarioBruto;

    @UISchema(label = "Total Descontos", type = FieldDataType.NUMBER, helpText = "Soma total de descontos aplicados.", icon = "money_off")
    @Schema(
            description = "Soma de descontos aplicados (encargos, beneficio, eventos de debito) no snapshot.",
            example = "2200.00")
    private BigDecimal totalDescontos;

    @UISchema(label = "Salário Líquido", type = FieldDataType.NUMBER, helpText = "Valor final depositado.", icon = "account_balance_wallet")
    @Schema(
            description = "Liquido a depositar apos a composicao de proventos e descontos desta visao.",
            example = "9800.00")
    private BigDecimal salarioLiquido;

    @UISchema(label = "Valor Adicionais", type = FieldDataType.NUMBER, helpText = "Soma dos adicionais pagos.", icon = "add_card")
    @Schema(
            description = "Parcela de proventos extras (adicionais) agregada para comparacao com o contracheque detalhado de eventos.")
    private BigDecimal valorAdicionais;

    @UISchema(label = "Valor Descontos Eventos", type = FieldDataType.NUMBER, helpText = "Descontos provenientes de eventos na folha.", icon = "remove_circle")
    @Schema(
            description = "Total de descontos originados de rubricas de eventos de folha (vs. desconto generico de imposto).")
    private BigDecimal valorDescontosEventos;

    @UISchema(label = "Valor Proventos", type = FieldDataType.NUMBER, helpText = "Proventos provenientes de eventos na folha.", icon = "paid")
    @Schema(
            description = "Agregado de proventos por rubrica na mesma janela; reconcilia com qtd e tipos de evento.")
    private BigDecimal valorProventos;

    @UISchema(label = "Saldo Eventos", type = FieldDataType.NUMBER, helpText = "Saldo líquido apenas de eventos extras.", icon = "account_balance")
    @Schema(
            description = "Saldo liquido dos eventos (provento menos desconto de eventos) conforme a definicao da materializacao (demo).")
    private BigDecimal saldoEventos;

    @UISchema(label = "% Desconto", type = FieldDataType.NUMBER, helpText = "Proporção de desconto sobre o salário bruto.", icon = "percent")
    @Schema(
            description = "Percentual de desconto face ao bruto; util para deteccao de anomalias e faixas de corte.")
    private BigDecimal pctDesconto;

    @UISchema(label = "% Líquido", type = FieldDataType.NUMBER, helpText = "Proporção líquida sobre o salário bruto.", icon = "percent")
    @Schema(
            description = "Percentual do liquido em relacao ao bruto; sumariza o peso de retencoes e encargos.")
    private BigDecimal pctLiquido;

    @UISchema(label = "Qtd Eventos", type = FieldDataType.NUMBER, helpText = "Quantidade total de eventos extras lançados.", icon = "format_list_numbered")
    @Schema(
            description = "Contagem de linhas de evento de folha consideradas na agregacao (rubrica).",
            example = "8")
    private Long qtdEventos;

    @UISchema(label = "Qtd Adicionais", type = FieldDataType.NUMBER, helpText = "Quantidade de eventos adicionais lançados.", icon = "playlist_add")
    @Schema(
            description = "Quantidade de itens classificados como adicionais de folha; cruza com Valor Adicionais.")
    private Long qtdAdicionais;

    @UISchema(label = "Perfil Folha", helpText = "Perfil agrupado de classificação folha/benefícios.", icon = "manage_accounts")
    @Schema(
            description = "Bucket de perfil de remuneracao (ex.: clt-heroi, misso) para analises e segmentacao de politica; valor de option-source na vista.")
    private String payrollProfile;

    @UISchema(label = "Composição Folha", helpText = "Perfil de composição qualitativa de ganhos.", icon = "donut_large")
    @Schema(
            description = "Composicao qualitativa da folha (ex.: provento-preponderante vs desconto-elevado); classificacao derivada, nao regra de calculo contabil (demo).")
    private String composicaoFolha;

    @UISchema(label = "Faixa Salário Bruto", helpText = "Agrupamento analítico da faixa do salário bruto.", icon = "stacked_bar_chart")
    @Schema(description = "Gama de bruto (faixa) para histogramas e comparativos de mercado interno; rotulo de bucket.")
    private String faixaSalarioBruto;

    @UISchema(label = "Faixa Salário Líquido", helpText = "Agrupamento analítico da faixa do líquido pago.", icon = "stacked_bar_chart")
    @Schema(description = "Gama de liquido relativa ao risco de retencao e beneficio; bucket na vista.")
    private String faixaSalarioLiquido;

    @UISchema(label = "Faixa % Desconto", helpText = "Agrupamento analítico do percentual retido.", icon = "stacked_bar_chart")
    @Schema(description = "Gama de intensidade de desconto; destaca casos de folha muito onerada (demo).")
    private String faixaPctDesconto;

    @UISchema(label = "Faixa Valor Adicionais", helpText = "Agrupamento analítico de valores extras concedidos.", icon = "stacked_bar_chart")
    @Schema(description = "Gama de adicionais agregados; compara com faixa de bruto/liquido para equidade interna.")
    private String faixaValorAdicionais;

    public Integer getFolhaPagamentoId() { return folhaPagamentoId; }
    public void setFolhaPagamentoId(Integer folhaPagamentoId) { this.folhaPagamentoId = folhaPagamentoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getUniverso() { return universo; }
    public void setUniverso(String universo) { this.universo = universo; }
    public Boolean getExposicaoPublica() { return exposicaoPublica; }
    public void setExposicaoPublica(Boolean exposicaoPublica) { this.exposicaoPublica = exposicaoPublica; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public LocalDate getCompetencia() { return competencia; }
    public void setCompetencia(LocalDate competencia) { this.competencia = competencia; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public BigDecimal getSalarioBruto() { return salarioBruto; }
    public void setSalarioBruto(BigDecimal salarioBruto) { this.salarioBruto = salarioBruto; }
    public BigDecimal getTotalDescontos() { return totalDescontos; }
    public void setTotalDescontos(BigDecimal totalDescontos) { this.totalDescontos = totalDescontos; }
    public BigDecimal getSalarioLiquido() { return salarioLiquido; }
    public void setSalarioLiquido(BigDecimal salarioLiquido) { this.salarioLiquido = salarioLiquido; }
    public BigDecimal getValorAdicionais() { return valorAdicionais; }
    public void setValorAdicionais(BigDecimal valorAdicionais) { this.valorAdicionais = valorAdicionais; }
    public BigDecimal getValorDescontosEventos() { return valorDescontosEventos; }
    public void setValorDescontosEventos(BigDecimal valorDescontosEventos) { this.valorDescontosEventos = valorDescontosEventos; }
    public BigDecimal getValorProventos() { return valorProventos; }
    public void setValorProventos(BigDecimal valorProventos) { this.valorProventos = valorProventos; }
    public BigDecimal getSaldoEventos() { return saldoEventos; }
    public void setSaldoEventos(BigDecimal saldoEventos) { this.saldoEventos = saldoEventos; }
    public BigDecimal getPctDesconto() { return pctDesconto; }
    public void setPctDesconto(BigDecimal pctDesconto) { this.pctDesconto = pctDesconto; }
    public BigDecimal getPctLiquido() { return pctLiquido; }
    public void setPctLiquido(BigDecimal pctLiquido) { this.pctLiquido = pctLiquido; }
    public Long getQtdEventos() { return qtdEventos; }
    public void setQtdEventos(Long qtdEventos) { this.qtdEventos = qtdEventos; }
    public Long getQtdAdicionais() { return qtdAdicionais; }
    public void setQtdAdicionais(Long qtdAdicionais) { this.qtdAdicionais = qtdAdicionais; }
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
    public String getFaixaValorAdicionais() { return faixaValorAdicionais; }
    public void setFaixaValorAdicionais(String faixaValorAdicionais) { this.faixaValorAdicionais = faixaValorAdicionais; }
}
