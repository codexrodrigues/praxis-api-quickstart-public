# ADR: Analitica de afastamentos para UI generativa governada

## Status

Aceito em 2026-07-15 para a prova operacional G2 da issue #98.

A issue permanece aberta porque ainda nao existe uma fonte confiavel e uma carga auditavel para
as lotacoes historicas dos afastamentos existentes. O piloto nao infere o departamento historico a
partir do departamento atual.

## Contexto

O pedido de referencia e mostrar afastamentos por departamento, comparar com o periodo anterior e
permitir abrir os funcionarios criticos. A decisao precisa ser reproduzivel por backend, Config,
Angular e IA sem transportar detalhes medicos, motivo do afastamento ou regras derivadas no
frontend.

A implementacao anterior fatiava cada evento por competencia. Eventos sobrepostos podiam contar o
mesmo dia mais de uma vez, e a identidade analitica permanecia acoplada ao evento de origem mesmo
quando a decisao de produto trata de uma pessoa em um departamento efetivo durante um mes.

## Inventario de aderencia

| Necessidade | Classificacao | Fonte canonica |
| --- | --- | --- |
| Comparacao da mesma metrica entre dois periodos | `ja-suportado-mal-nomeado-ou-mal-materializado` | operacao `statsComparison` do Metadata Starter |
| Graficos, tabelas, filtros, cross-filter e composicao | `ja-suportado-so-ux` | runtimes oficiais `@praxisui/*` |
| Projecao por pessoa, lotacao efetiva e competencia | `suportado-parcialmente`, materializado neste piloto | dominio Human Resources do Quickstart |
| Policy de criticidade versionada | `suportado-parcialmente`, materializado neste piloto | funcao PostgreSQL e identidade de policy do dominio |
| Autorizacao aggregate, nominal e employee-360 | `ja-suportado-so-ux`, materializado no host | Spring Security e `ResourceOperationAvailabilityProvider` |
| Binding da key agregada para filtro nominal | `lacuna-real-de-contrato` encerrada no Metadata `rc.110` | `AnalyticsDimensionBinding.keyFilterField` |
| Alvo cross-resource da linha nominal para employee-360 | `lacuna-real-de-contrato` encerrada no Metadata `rc.110` | `AnalyticsRecordOpen` e catalogo de surfaces |
| Backfill de lotacao historica real | lacuna de dados de negocio, nao de plataforma | sistema corporativo de RH |

Config e Angular devem consumir esses contratos sem inferir filtros por nomes e sem reconstruir
paths de surface. `/schemas/surfaces` continua owner de path, schema, scope e availability; o
`recordOpen` publica apenas a referencia semantica minima ao item real do catalogo.

## Decisao analitica

O recurso `human-resources.vw-analytics-afastamentos` publica o grao:

`funcionarioId x departamentoId efetivo x competencia mensal`

`funcionario_lotacoes_departamento` e a unica fonte de atribuicao departamental. Sua validade e
half-open, `[effective_from, effective_to)`. Cada dia do afastamento e associado a lotacao valida
naquele dia, portanto uma mudanca de departamento pode gerar linhas analiticas distintas.

A view expande os afastamentos para dias de calendario, associa a lotacao efetiva, elimina dias
duplicados no grao publico e so entao agrega. `diasAfastado` e a uniao de dias cobertos, nunca a
soma cega das duracoes dos eventos.

`periodoInicio` e `periodoFim` representam o envelope da uniao. Eles nao afirmam que todos os
dias intermediarios foram dias de afastamento.

Sem lotacao efetiva existe o diagnostico deterministico
`DEPARTMENT_ASSIGNMENT_MISSING` e nenhuma linha analitica. Nao ha fallback para
`funcionarios.departamento_id`.

A comparacao padrao usa o mes calendario corrente contra o mes calendario anterior em
`America/Sao_Paulo`. O request canonico usa:

- `DISTINCT_COUNT(funcionarioId)` para pessoas;
- `SUM(diasAfastado)` para dias unicos ja materializados;
- dimensao `departamento`;
- uniao e zero-fill de buckets pela operacao do Metadata Starter.

## Policy de criticidade

A policy canonica do piloto e:

- `policyId`: `hr-absence-criticality-v1`;
- `policyVersion`: `2026-07-15`;
- `STANDARD`: de 0 a 6 dias unicos;
- `ATTENTION`: de 7 a 14 dias unicos;
- `CRITICAL`: 15 dias unicos ou mais.

`public.hr_absence_criticality_level(bigint)` e o unico owner executavel dos limites usado pela
view. Java publica somente a identidade e a versao da policy. A IA nao recria thresholds por prompt,
label ou configuracao incidental.

## Autorizacao e escopo

As autoridades do piloto sao:

- `HR_ANALYTICS_AGGREGATE_READ`: comparacao agregada;
- `HR_ANALYTICS_NOMINAL_READ`: linhas e detalhes da projecao;
- `HR_EMPLOYEE_360_READ`: superficie oficial do funcionario.

`ROLE_ADMIN` recebe as tres no login demonstrativo. Os caminhos permanecem protegidos mesmo com
`read-open=true`.

A collection capability de `vw-analytics-afastamentos` pode ser descoberta por principals
aggregate ou nominal. Linhas continuam protegidas pela autoridade nominal, e o hero profile exige
employee-360.

`HrAnalyticsResourceOperationAvailabilityProvider` projeta a mesma separacao no snapshot:

- `statsComparison` exige `HR_ANALYTICS_AGGREGATE_READ`;
- `filter`, `cursor`, options, export e os demais stats exigem `HR_ANALYTICS_NOMINAL_READ`;
- uma negacao publica `missing-authority` e metadata sanitizada da policy;
- `canonicalOperations` permanece presenca estrutural e nunca vira autorizacao.

O provider orienta discovery e authoring. O `SecurityFilterChain` continua sendo a barreira
executavel e e testado separadamente para impedir bypass por `read-open=true`.

`HrDepartmentScopeProvider` resolve o escopo no servidor. O catalogo demonstrativo usa
`app.hr.analytics.demo-department-scopes`:

- IDs de departamento sao separados por `|`;
- `*` declara escopo irrestrito explicitamente;
- subject desconhecido resolve para deny-all;
- o admin configurado permanece irrestrito, salvo entrada explicita no catalogo;
- escopo vazio, intersecao vazia e departamento escalar fora do entitlement retornam `403`.

Hosts corporativos substituem esse bean por IAM ou entitlements. Ocultar um controle na UI nunca
substitui a negativa do endpoint.

No filtro nominal, o `FilterDTO` continua sendo apenas a pergunta funcional controlada pelo
cliente. `HrDepartmentScopeAccess` resolve separadamente um `ResourceFilterAccessScope` a partir do
principal autenticado; a base do Metadata Starter combina esse limite com a pagina normal e o
reaplica ao carregar `includeIds`. Assim, uma selecao autorizada fora do filtro corrente pode ser
reidratada, mas um ID de outro departamento nao atravessa a fronteira de acesso.

## Governanca AI-safe

A projecao destinada ao grounding pode publicar competencia, identidade do funcionario,
departamento efetivo, dias, criticidade e identidade da policy. `tipo` e `observacoes` nao
participam da policy e nao podem entrar em contexto AI-safe, streaming, diagnosticos ou
configuracao persistida.

A ausencia de policy aplicavel ou de capability deve produzir clarificacao, indisponibilidade
explicavel ou resultado inconclusivo. A LLM nao pode inventar um threshold nem ampliar o escopo do
principal.

## Composicao generativa esperada

O oraculo semantico fixa estes widgets:

- `absence-summary`;
- `absence-period-filter`;
- `absence-department-comparison-chart`;
- `absence-critical-employees-table`.

Os bindings esperados sao:

- periodo para `queryContext` de grafico e tabela;
- `primaryDimension.keyFilterField=departamentoIdsIn`, recebendo `bucket.key` sem usar o label;
- `crossFilter` do grafico para o filtro departamental da tabela;
- `recordOpen.sourceIdentityField=funcionarioId` na linha nominal;
- `recordOpen.target.resourceKey=human-resources.funcionarios`;
- `recordOpen.target.surfaceId=hero-profile`.

Labels de departamento nao podem atuar como identidade quando `departamentoId` existe. O bucket
agregado nao carrega `funcionarioId` e, portanto, nao abre diretamente uma surface ITEM. O fluxo
canonico e bucket -> cross-filter da lista nominal -> selecao da linha -> resolucao do item real em
`/schemas/surfaces` -> `surface.open` com a identidade publicada.

## Golden contracts

Os oraculos foram separados por responsabilidade para evitar que o contrato semantico preliminar e
a prova operacional compartilhem acidentalmente o mesmo formato.

### Oraculo semantico

- `src/test/resources/absence-analytics-lab/absence-analytics-semantic-golden-suite.json`;
- `src/test/resources/absence-analytics-lab/absence-analytics-semantic-golden-suite.schema.json`;
- `hr.analytics.AbsenceAnalyticsGoldenContractTest`.

Ele prova metricas, periodo, uniao de dias, mudanca de lotacao half-open, campos AI-safe, perfis de
acesso, widgets, bindings e abertura da superficie oficial. Seu calculo e independente do codigo de
producao.

### Oraculo operacional

- `src/test/resources/absence-analytics-lab/absence-analytics-golden-suite.json`;
- `src/test/resources/absence-analytics-lab/absence-analytics-golden-suite.schema.json`;
- `config.AbsenceAnalyticsGoldenContractTest`.

Ele prova o grao G2, policy versionada, eventos sobrepostos, mudanca de departamento, ausencia de
lotacao, bucket apenas no periodo anterior e envelope de dias nao contiguos.

A fixture PostgreSQL compartilhada executa as migrations operacionais em Testcontainers. Uma prova
valida as linhas, a funcao de policy e os grants; outra inicia o host no mesmo banco migrado e
valida a resposta HTTP autorizada. Nenhum teste usa dados de producao.

## Ownership

- Quickstart: policy concreta do dominio, migration, fixture, seguranca do host e prova HTTP;
- Metadata Starter: comparacao, schemas, capabilities, surfaces e vocabulario de field access;
- Config Starter: grounding e authoring governado da composicao;
- Angular: runtime de decisoes materializadas;
- Landing e corpus HTTP: evidencias derivadas, nunca fontes de verdade.

## Consequencias

`analyticsId` e estavel para o grao publico e nao expoe `afastamentoId`. Como a plataforma esta
em beta, esta e uma migracao canonica limpa, sem view v1/v2, alias ou endpoint paralelo.

A prova G2 esta implementada, mas nao autoriza backfill por inferencia. A decisao de negocio, a
fonte confiavel e a carga auditavel das lotacoes historicas reais pertencem ao projeto de migracao
do Ergon, rastreadas em [Techne-ErgonX-migracao#133](https://github.com/codexrodrigues/Techne-ErgonX-migracao/issues/133);
essa continuidade nao e um contrato pendente do Quickstart nem bloqueia o fechamento da issue #98
como prova de plataforma.

A lista nominal e o employee-360 nao devem ser codificados localmente no Config. O Metadata agora
publica os bindings minimos; Config e Angular precisam resolver os catalogos e availability
existentes e falhar fechado quando o principal nao puder executar a proxima etapa.

## Alternativas rejeitadas

1. Somar duracoes dos eventos: duplica dias sobrepostos.
2. Usar o departamento atual: falsifica a atribuicao historica.
3. Calcular comparacao no Angular: desloca semantica temporal e zero-fill para um consumidor.
4. Persistir apenas `critical=true`: perde policy, versao e provenance.
5. Inferir criticidade de `tipo` ou `observacoes`: viola dominio, governanca e privacidade.
6. Criar rota hardcoded de funcionario no Config: ignora Metadata, surfaces e capabilities.

## Gates

- G0, decisao e oraculo semantico: concluido.
- G1, contrato generico de comparacao no Metadata: concluido.
- G2, projecao operacional e provas PostgreSQL/HTTP: concluido no codigo e nos gates.
- Reabertura focal da issue #98: concluida pelas correcoes do executor PostgreSQL e do row scope de
  `includeIds`; o backfill historico confiavel permanece na issue Ergon #133.
- G3, contrato backend canonico: concluido no Metadata `rc.110` e adotado no Quickstart.
- G3, materializacao generativa completa: pendente da prova integrada em Config, Angular, Landing
  e corpus.
