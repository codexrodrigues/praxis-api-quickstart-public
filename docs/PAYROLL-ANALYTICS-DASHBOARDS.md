# Payroll Analytics Dashboards

Guia pratico para explorar `vw_analytics_folha_pagamento` via `/stats/*` e montar dashboards diferentes sobre o mesmo recurso.

Recursos canonicos:

- `POST /api/human-resources/vw-analytics-folha-pagamento/stats/group-by`
- `POST /api/human-resources/vw-analytics-folha-pagamento/stats/timeseries`
- `POST /api/human-resources/vw-analytics-folha-pagamento/stats/distribution`
- `POST /api/human-resources/vw-analytics-folha-pagamento/option-sources/{sourceKey}/options/filter`
- `GET /api/human-resources/vw-analytics-folha-pagamento/option-sources/{sourceKey}/options/by-ids`

Campos mais uteis para dashboards:

- dimensoes: `ano`, `mes`, `competencia`, `cargo`, `departamento`, `equipe`, `base`, `universo`, `payrollProfile`, `composicaoFolha`, `faixaSalarioBruto`, `faixaSalarioLiquido`, `faixaPctDesconto`, `faixaValorAdicionais`
- metricas: `salarioBruto`, `totalDescontos`, `salarioLiquido`, `valorProventos`, `valorDescontosEventos`, `valorAdicionais`, `saldoEventos`, `saldoLiquidoVsBruto`, `pctDesconto`, `pctLiquido`, `pctAdicionaisSobreBruto`, `pctEventosDescontoSobreBruto`, `qtdEventos`, `qtdAdicionais`

## Fontes canonicas de filtro derivado

O recurso publica `option-sources` para campos categoricos derivados que nao possuem CRUD proprio.

Casos ja governados:

- `universo`
- `payrollProfile`
- `composicaoFolha`
- `faixaSalarioBruto`
- `faixaSalarioLiquido`
- `faixaPctDesconto`

Exemplo:

```http
POST /api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/filter?search=OPER&page=0&size=10
Content-Type: application/json

{
  "universo": "Marvel",
  "competenciaBetween": ["2025-01-01", "2025-12-31"]
}
```

Uso:

- preencher `async-select` metadata-driven sem endpoint ad hoc
- respeitar o mesmo filtro global do dashboard
- reidratar selecoes via `/option-sources/{sourceKey}/options/by-ids`

## 1. Evolucao do Liquido Pago

Visual:

- line chart
- area chart

Payload:

```json
{
  "filter": {},
  "field": "competencia",
  "granularity": "MONTH",
  "metric": { "operation": "SUM", "field": "salarioLiquido" },
  "fillGaps": false
}
```

Uso:

- evolucao da massa salarial liquida
- tendencia mensal por competencia

## 2. Perfil da Folha

Visual:

- bar chart
- donut chart

Payload:

```json
{
  "filter": {},
  "field": "payrollProfile",
  "metric": { "operation": "COUNT" },
  "orderBy": "VALUE_DESC"
}
```

Variacoes uteis:

- `field = "composicaoFolha"`
- `field = "departamento"`
- `field = "equipe"`

## 3. Faixas de Desconto

Visual:

- stacked bar
- donut

Payload:

```json
{
  "filter": {},
  "field": "faixaPctDesconto",
  "metric": { "operation": "COUNT" },
  "orderBy": "KEY_ASC"
}
```

Uso:

- comparar concentracao de folhas em faixas de desconto
- monitorar aumento de pressao sobre a folha ao longo do tempo

## 4. Distribuicao do Salario Liquido

Visual:

- histogram
- boxplot derivado

Payload:

```json
{
  "filter": {},
  "field": "salarioLiquido",
  "mode": "HISTOGRAM",
  "metric": { "operation": "COUNT" },
  "bucketSize": 5000,
  "orderBy": "KEY_ASC"
}
```

Variacoes uteis:

- `field = "salarioBruto"`
- `field = "pctDesconto"`
- `field = "valorAdicionais"`

## 5. Massa Salarial por Departamento

Visual:

- horizontal bar
- treemap

Payload:

```json
{
  "filter": {},
  "field": "departamento",
  "metric": { "operation": "SUM", "field": "salarioLiquido" },
  "orderBy": "VALUE_DESC",
  "limit": 12
}
```

Uso:

- ranking de departamentos mais caros
- treemap de participacao da folha por unidade

## 6. Adicionais por Perfil

Visual:

- grouped bar
- dot plot

Payload:

```json
{
  "filter": {},
  "field": "payrollProfile",
  "metric": { "operation": "AVG", "field": "pctAdicionaisSobreBruto" },
  "orderBy": "VALUE_DESC"
}
```

Uso:

- comparar o peso relativo dos adicionais entre perfis
- destacar `SECURITY`, `OPERATIONS` e `EXEC`

## 7. Volatilidade por Equipe

Visual:

- line chart por filtro de equipe
- ranked bars com drill-down mensal

Payload base:

```json
{
  "filter": {},
  "field": "equipe",
  "metric": { "operation": "AVG", "field": "salarioLiquido" },
  "orderBy": "VALUE_DESC"
}
```

Drill-down temporal:

```json
{
  "filter": { "equipe": "Vingadores" },
  "field": "competencia",
  "granularity": "MONTH",
  "metric": { "operation": "AVG", "field": "salarioLiquido" },
  "fillGaps": false
}
```

## 8. Pressao de Desconto ao Longo do Tempo

Visual:

- line chart
- combo chart

Payload:

```json
{
  "filter": {},
  "field": "competencia",
  "granularity": "MONTH",
  "metrics": [
    { "operation": "SUM", "field": "salarioLiquido", "alias": "massaLiquida" },
    { "operation": "AVG", "field": "pctDesconto", "alias": "mediaPctDesconto" }
  ],
  "fillGaps": false
}
```

Uso:

- acompanhar crescimento percentual dos descontos
- projetar `massaLiquida` como barras e `mediaPctDesconto` como linha no mesmo `combo`
- consumir o mesmo envelope canonico remoto de `praxis.stats`, sem montagem manual no frontend

## 9. Rankings Individuais

Visual:

- sorted bar chart
- table with sparkline

Payload:

```json
{
  "filter": {
    "anoBetween": [2025, 2025]
  },
  "field": "nomeCompleto",
  "metric": { "operation": "SUM", "field": "salarioLiquido" },
  "orderBy": "VALUE_DESC",
  "limit": 15
}
```

Uso:

- top recebedores por ano
- ranking por universo, equipe ou cargo via filtros

## Filtros Recomendados

Filtros com melhor custo-beneficio analitico:

- `anoBetween`
- `mesBetween`
- `competenciaBetween`
- `cargo`
- `departamento`
- `equipe`
- `base`
- `universo`
- `payrollProfile`
- `composicaoFolha`
- `faixaSalarioBruto`
- `faixaSalarioLiquido`
- `faixaPctDesconto`
- `salarioLiquidoBetween`
- `pctDescontoBetween`
- `valorAdicionaisBetween`

## Combinacoes Boas de Dashboard

Dashboard executivo:

- KPI de `SUM(salarioLiquido)`
- serie mensal de `SUM(salarioLiquido)`
- bar de `SUM(salarioLiquido)` por `departamento`
- donut de `COUNT` por `payrollProfile`

Dashboard de risco financeiro:

- serie de `AVG(pctDesconto)`
- histogram de `pctDesconto`
- bar de `AVG(pctDesconto)` por `departamento`
- distribuicao de `valorAdicionais`

Dashboard operacional:

- serie de `AVG(salarioLiquido)` por filtro de `equipe`
- bar de `AVG(pctAdicionaisSobreBruto)` por `payrollProfile`
- group-by de `composicaoFolha`
- histogram de `qtdEventos`
