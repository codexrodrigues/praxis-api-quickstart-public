# Cockpit como referencia do Quickstart

Este guia explica como ler o `praxis-api-quickstart` pelo Praxis Cockpit e como
evoluir o host de exemplo para demonstrar, em HTTP real, as capacidades centrais
da plataforma Praxis: dominio, recursos, schemas, filtros, tabelas, formularios,
analytics, relationships, surfaces, actions e workflows.

O Quickstart nao e a fonte canonica dos contratos Praxis. A fonte canonica de
metadata-driven backend continua sendo o `praxis-metadata-starter`; o Quickstart
e o host operacional que prova como esses contratos aparecem em um servico real.

## Evidencia publica

- Cockpit: https://praxis-api-quickstart.onrender.com/praxis/cockpit
- Build publicado: https://praxis-api-quickstart.onrender.com/actuator/info
- Catalogo de endpoints: https://praxis-api-quickstart.onrender.com/schemas/catalog
- OpenAPI por grupo:
  - https://praxis-api-quickstart.onrender.com/v3/api-docs/human-resources
  - https://praxis-api-quickstart.onrender.com/v3/api-docs/operations
  - https://praxis-api-quickstart.onrender.com/v3/api-docs/procurement
  - https://praxis-api-quickstart.onrender.com/v3/api-docs/assets
  - https://praxis-api-quickstart.onrender.com/v3/api-docs/risk-intelligence

Use sempre `/praxis/cockpit` como link permanente. Parametros como `release`,
`published` e `qa` sao apenas cache-busters de validacao.

## O que o cockpit deve responder

Ao abrir o cockpit, uma pessoa tecnica, produto ou IA deve conseguir responder:

- Que dominio este servico governa?
- Quais areas de negocio existem e quantos recursos cada uma publica?
- Quais recursos sao transacionais, views analiticas, lookups ou action hosts?
- Quais endpoints existem para leitura, escrita, filtros, options e stats?
- Quais tabelas, formularios, filtros, charts e workflows podem ser materializados?
- Quais actions existem, que decisao de negocio executam e que evidencias sustentam isso?
- Que recursos se conectam a outros recursos por DTOs, surfaces, actions, ratios ou relationships?
- O que esta pronto para IA/runtime e o que ainda precisa de melhoria no host?

Se a resposta depender de leitura de codigo-fonte ou memoria do exemplo, a
documentacao do host ainda esta fraca.

## Inventario atual por dominio

Inventario gerado a partir do codigo anotado e dos grupos OpenAPI publicados em
producao.

| Dominio no cockpit | Grupo OpenAPI | Recursos | Surfaces explicitas | Workflow actions explicitas | Leitura de aderencia |
| --- | --- | ---: | ---: | ---: | --- |
| Pessoas e RH | `human-resources` | 21 | 12 | 14 | `ja-suportado-mal-nomeado-ou-mal-materializado`: perfil 360, folha, participacoes em missoes, dependentes, endereco cadastral, matriz de competencias, historico de cargos, disponibilidade por afastamentos, actions de ciclo do funcionario, actions de folha/eventos, action de cobertura, lifecycle persistente e shadow sanitizado de beneficio extraordinario, ranking reputacional e governanca de codigos legados de folha ja existem; a proxima melhoria deve aprofundar operacao sem criar surface decorativa. |
| Operacoes | `operations` | 12 | 12 | 10 | `ja-suportado-mal-nomeado-ou-mal-materializado`: e o melhor dominio para demonstrar actions/surfaces; cockpit deve usar esse dominio como referencia visual de workflows, composicao de equipes, capacidade operacional e ponte transacional para leituras analiticas derivadas. |
| Suprimentos | `procurement` | 5 | 10 | 8 | `ja-suportado-mal-nomeado-ou-mal-materializado`: recursos, schemas, option sources, surfaces e actions de fornecedor, contrato e pedido existem; o cockpit deve materializar a jornada fornecedor -> contrato -> catalogo -> pedido -> recebimento como fluxo navegavel. |
| Ativos Operacionais | `assets` | 4 | 6 | 7 | `ja-suportado-mal-nomeado-ou-mal-materializado`: recursos, lookups, surfaces, actions de disponibilidade e actions de custodia existem; o cockpit deve materializar inventario -> custodia -> frota -> missao -> devolucao/perda/dano como fluxo navegavel. |
| Inteligencia de Risco | `risk-intelligence` | 2 | 3 | 2 | `suportado-parcialmente`: ameacas publicam surface, actions reais de triagem e stats; incidentes ja existem como recurso transacional em `operations.incidentes`, agora com surface item-level para abrir `risk-intelligence.vw-indicadores-incidentes` como leitura analitica derivada e chart de tendencia. |

Nenhum item acima exige contrato novo neste momento. A plataforma ja sabe expor
`@ApiResource`, `@Operation`, `@Schema`, `@UISchema`, stats, options, surfaces,
actions e capabilities. Os numeros da tabela devem ser conferidos com
`scripts/verify-cockpit-inventory-doc.sh`, que compara este guia com o OpenAPI e
as surfaces/actions publicadas no runtime. O contrato materializavel das actions
deve ser conferido com `scripts/verify-cockpit-action-contracts.sh`, que garante
path OpenAPI e schemas filtrados de request/response para cada workflow action
publicada. O contrato materializavel das surfaces semanticas deve ser conferido
com `scripts/verify-cockpit-surface-contracts.sh`, que garante titulo, descricao,
path OpenAPI e schema filtrado para cada experiencia composta que o cockpit deve
conseguir abrir, explicar ou transformar em visao navegavel. Os relacionamentos
navegaveis usados pelo diagrama do cockpit devem ser conferidos com
`scripts/verify-cockpit-related-resource-contracts.sh`, que valida `relatedResource`
contra surface, schema, campo pai, chave de selecao e operacoes OpenAPI do recurso
filho quando a surface aponta para um filho CRUD real. Os contratos de analytics
devem ser conferidos com `scripts/verify-cockpit-analytics-contracts.sh`, que
valida endpoints `/stats/*`, schemas filtrados de request/response e surfaces
`CHART` com projecoes `praxis.stats`. Os lookups governados devem ser conferidos
com `scripts/verify-cockpit-option-source-contracts.sh`, que descobre
`x-ui.optionSource` em schemas filtrados e valida endpoints de busca e reidratacao
para filtros e formularios. Os contratos estruturais de UI devem ser conferidos
com `scripts/verify-cockpit-structural-ui-contracts.sh`, que valida schemas
filtrados materializaveis para leitura, filtros, tabelas, criacao e edicao quando
essas operacoes existem no OpenAPI. A lacuna principal e de exemplaridade no
host: quando um dominio ainda parecer fraco no cockpit, a primeira pergunta deve
ser se ja existe semantica publicada que o dashboard ainda nao esta materializando
bem antes de criar novos comandos ou campos.

## Como cada camada aparece no cockpit

### Dominio e areas

Fonte no Quickstart:

- `src/main/java/com/example/praxis/apiquickstart/constants/ApiPaths.java`
- controllers com `@ApiResource(resourceKey=...)`
- grupos OpenAPI publicados por dominio

Materializacao no cockpit:

- mapa semantico por area;
- total de recursos por dominio;
- relacao entre endpoints tecnicos e leitura de dominio;
- filtro por area para navegar recursos.

### Recursos e contratos

Fonte no Quickstart:

- `@ApiResource` para identidade publica;
- `@Operation` para finalidade de cada endpoint;
- DTOs e filtros com `@Schema(description=...)` e `@UISchema(...)`;
- Bean Validation e tipos Java usados pelo starter para enriquecer contrato.

Materializacao no cockpit:

- lista de recursos;
- prontidao semantica por recurso;
- endpoints de leitura/escrita/filtro/options;
- possibilidades de tabela, formulario, filtro, chart, lookup e workflow.

### Surfaces

Fonte no Quickstart:

- `@UiSurface` nos controllers.

Uso esperado:

- nomear uma experiencia composta que nao e apenas CRUD;
- explicar contexto de uso, tipo de UI e operacoes suportadas;
- permitir que o cockpit mostre "o que pode ser montado" com linguagem de negocio.

Exemplos atuais mais fortes:

- `operations.missoes`: surfaces para acompanhamento, analytics e operacao;
- `operations.equipes`: composicao de membros e capacidade operacional navegavel;
- `operations.incidentes`: surface de investigacao e ponte navegavel para indicadores analiticos de risco;
- `procurement.companies`: surface de escopo de empresas compradoras;
- `procurement.suppliers`: surface de homologacao, risco, elegibilidade, contratos e pedidos vinculados;
- `procurement.contracts`: surface de governanca contratual, produtos cobertos e pedidos emitidos sob o contrato;
- `procurement.products`: surface de catalogo de produtos contratados;
- `procurement.purchase-orders`: surface de jornada governada empresa -> fornecedor -> contrato -> produto -> pedido, com actions de aprovar, cancelar e receber pedido;
- `assets.equipamentos`: surface de inventario e historico de custodia navegavel;
- `assets.equipamento-alocacoes`: surface de cadeia de custodia e actions de devolucao, perda e dano;
- `assets.veiculos`: surface de prontidao da frota e uso em missoes navegavel;
- `human-resources.funcionarios`: perfil, folha, missoes, dependentes, endereco cadastral, competencias e historico de cargos como composicao navegavel para gestao de pessoas, beneficios, atendimento de RH, capacidade, carreira e privacidade;
- `operations.base-acessos`: revisao e governanca de acesso.

### Actions e workflows

Fonte no Quickstart:

- `@WorkflowAction` nos controllers.

Uso esperado:

- publicar comandos de negocio que nao devem ficar escondidos como PATCH generico;
- informar status, efeito esperado, alvo, payload e contexto;
- permitir que IA, cockpit e runtime distingam leitura, edicao e decisao operacional.

Exemplos atuais mais fortes:

- folha de pagamento: aprovar ou revisar ciclos;
- funcionarios: inativar e reativar vinculo funcional com trilha de auditoria;
- codigos legados de folha: duplicar rascunho para migracao e saneamento controlado;
- missoes: transicoes operacionais de ciclo;
- acessos a bases: ativar/desativar autorizacao;
- acordos regulatorios: revisar/ativar/suspender compromissos.
- suprimentos: bloquear/reintegrar fornecedor, assinar/suspender contrato e aprovar/cancelar/receber pedido.
- ativos: enviar equipamento ou veiculo para manutencao, devolver ao estoque/operação e encerrar custodia como devolvida, perdida ou danificada.

### Charts e analytics

Fonte no Quickstart:

- views analiticas;
- endpoints `stats/*`;
- descricoes de `@Operation` que expliquem dimensoes, metricas e serie temporal.

Recursos atuais com maior potencial:

- `human-resources.vw-analytics-folha-pagamento`;
- `operations.vw-resumo-missoes`;
- `risk-intelligence.vw-indicadores-incidentes`.
- `assets.equipamentos`;
- `assets.equipamento-alocacoes`;
- `assets.veiculos`;
- `assets.veiculo-missao-usos`.
- `procurement.purchase-orders`;
- `procurement.contracts`;
- `procurement.suppliers`;
- `procurement.products`;
- `procurement.companies`.

O cockpit deve preferir explicar charts por pergunta de negocio, por exemplo:
"Como a severidade de incidentes evolui por base?" em vez de apenas listar que
um endpoint `stats` existe.

Em `human-resources`, as perguntas ja materializadas pelo host exemplar sao:

- "Como a força de trabalho se distribui por status e cargo?" via
  `POST /api/human-resources/funcionarios/stats/group-by` com `field=ativo` ou
  `field=cargoNome`;
- "Qual a distribuição salarial do quadro?" via
  `POST /api/human-resources/funcionarios/stats/distribution` com
  `field=salario`;
- "Como a folha se comporta por departamento, perfil e competência?" via
  `POST /api/human-resources/vw-analytics-folha-pagamento/stats/group-by` e
  `POST /api/human-resources/vw-analytics-folha-pagamento/stats/timeseries`;
- "Quais universos e perfis 360 aparecem na base?" via
  `POST /api/human-resources/vw-perfil-heroi/stats/group-by` com
  `field=universo`;
- "Quais comandos operacionais existem na folha?" via
  `/schemas/actions?resource=human-resources.folhas-pagamento` e
  `/schemas/actions?resource=human-resources.eventos-folha`.
- "Uma solicitacao de beneficio extraordinario e elegivel, por qual regra e sob qual snapshot?" via
  `/schemas/actions?resource=human-resources.extraordinary-benefit-requests`,
  `GET /api/human-resources/extraordinary-benefit-requests/capabilities` e
  `POST /api/human-resources/extraordinary-benefit-requests/actions/evaluate`. No QL-05, o recurso
  publica query read-only e lifecycle por actions: somente `ALLOW` persiste, `submit/approve/apply`
  exigem ETag e idempotencia, e o efeito executado fica em ledger host-side exatamente uma vez;
- "Baseline e snapshot Praxis concordam sem produzir efeito?" via
  `POST /api/human-resources/extraordinary-benefit-requests/actions/shadow-compare`. A action QL-06
  é administrativa, não usa ledger idempotente e devolve somente observação sanitizada com
  `MATCH`, `MISMATCH`, `INCONCLUSIVE` ou `TECHNICAL_ERROR`;
- "Quem esta fora da operacao e em que janela de cobertura?" via
  `POST /api/human-resources/ferias-afastamentos/stats/group-by` com
  `field=tipo` ou `field=funcionarioId` e
  `POST /api/human-resources/ferias-afastamentos/stats/timeseries` com
  `field=dataInicio` ou `field=dataFim`;
- "Como a ausencia sera coberta operacionalmente?" via
  `/schemas/actions?resource=human-resources.ferias-afastamentos` e
  `POST /api/human-resources/ferias-afastamentos/{id}/actions/plan-coverage`;
- "Como reputacao se distribui por equipe, score e posicao?" via
  `POST /api/human-resources/vw-ranking-reputacao/stats/group-by` com
  `field=equipe` e `POST /api/human-resources/vw-ranking-reputacao/stats/distribution`
  com `field=scorePublico`, `field=scoreGovernamental`, `field=media` ou
  `field=posicao`.

O smoke `scripts/verify-human-resources-runtime.sh` protege essas evidencias no
host publicado e confirma as surfaces de perfil 360, historico de folha,
participacoes em missoes, dependentes, endereco cadastral, matriz de competencias,
historico de cargos, agenda de pagamento, calendario de disponibilidade, action
de cobertura, avaliacao deterministica de beneficio extraordinario, ranking reputacional, actions
de ciclo do funcionario e actions de folha/eventos.

Em `operations`, as perguntas ja materializadas pelo host exemplar sao:

- "Como missoes se distribuem por status e prioridade?" via
  `POST /api/operations/vw-resumo-missoes/stats/group-by` com `field=status`
  ou `field=prioridade`;
- "Quando as primeiras acoes das missoes ocorreram?" via
  `POST /api/operations/vw-resumo-missoes/stats/timeseries` com
  `field=primeiraAcao`;
- "Que tipos de eventos compoem a linha do tempo operacional?" via
  `POST /api/operations/missao-eventos/stats/group-by` com `field=tipo`;
- "Como eventos evoluem no tempo?" via
  `POST /api/operations/missao-eventos/stats/timeseries` com
  `field=ocorridoEm`;
- "Quais papeis compoem as equipes de missao?" via
  `POST /api/operations/missao-participantes/stats/group-by` com
  `field=papel`;
- "Quais comandos existem no ciclo de missao, acesso e acordo?" via
  `/schemas/actions?resource=operations.missoes`,
  `/schemas/actions?resource=operations.base-acessos` e
  `/schemas/actions?resource=operations.acordos-regulatorios`.

O smoke `scripts/verify-operations-runtime.sh` protege essas evidencias no host
publicado e confirma surfaces de acompanhamento de missao, composicao de equipes,
revisao de acesso, governanca de acordo, actions de ciclo operacional e charts de
status, prioridade, eventos, equipe e timeline.

Em `assets`, as perguntas ja materializadas pelo host exemplar sao:

- "Quantos equipamentos estao em estoque, uso, manutencao, quebrados ou perdidos?"
  via `POST /api/assets/equipamentos/stats/group-by` com `field=status`;
- "Qual a distribuicao de resistencia do inventario?" via
  `POST /api/assets/equipamentos/stats/distribution` com `field=resistencia`.
  A escala publicada e 1-10; use buckets pequenos, como `bucketSize=2`, para
  evitar colapsar toda a serie em uma unica faixa;
- "Quantas custodias estao ativas, devolvidas, perdidas ou danificadas?" via
  `POST /api/assets/equipamento-alocacoes/stats/group-by` com `field=status`;
- "Quando as custodias foram abertas ou encerradas?" via
  `POST /api/assets/equipamento-alocacoes/stats/timeseries` com `field=inicio`
  ou `field=fim`;
- "Como esta a prontidao da frota?" via
  `POST /api/assets/veiculos/stats/group-by` com `field=status`. O seed publico
  cobre veiculos operacionais, em manutencao e inoperantes;
- "Quais veiculos, missoes ou pilotos concentram uso de frota?" via
  `POST /api/assets/veiculo-missao-usos/stats/group-by` com `field=veiculoId`,
  `field=missaoId` ou `field=pilotoId`.

O smoke `scripts/verify-assets-runtime.sh` protege essas evidencias no host
publicado e confirma as surfaces `equipment-inventory-board`,
`equipment-custody-board`, `fleet-readiness-board` e
`mission-fleet-usage-board`.

Em `procurement`, as perguntas ja materializadas pelo host exemplar sao:

- "Como os pedidos se distribuem por status, fornecedor, contrato, produto ou moeda?" via
  `POST /api/procurement/purchase-orders/stats/group-by`;
- "Quando pedidos foram criados, aprovados, cancelados ou recebidos?" via
  `POST /api/procurement/purchase-orders/stats/timeseries` com `field=orderDate`,
  `field=approvedAt`, `field=cancelledAt` ou `field=receivedAt`;
- "Qual a distribuicao de quantidade dos pedidos?" via
  `POST /api/procurement/purchase-orders/stats/distribution` com `field=quantity`;
- "Quais contratos vencem por periodo e como se distribuem por fornecedor, moeda e status?" via
  `POST /api/procurement/contracts/stats/timeseries` com `field=validUntil` e
  `POST /api/procurement/contracts/stats/group-by`;
- "Como fornecedores se agrupam por homologacao, risco e status?" via
  `POST /api/procurement/suppliers/stats/group-by`;
- "Como produtos se agrupam por categoria, unidade, contrato, status e estoque?" via
  `POST /api/procurement/products/stats/group-by` e
  `POST /api/procurement/products/stats/distribution` com `field=stockAvailable`;
- "Onde estao concentradas as empresas compradoras?" via
  `POST /api/procurement/companies/stats/group-by` com `field=state`, `field=city`
  ou `field=status`.

Em risco e incidentes, as perguntas ja materializadas pelo host exemplar sao:

- "Quais severidades concentram incidentes operacionais?" via
  `POST /api/operations/incidentes/stats/group-by` com `field=severidade`;
- "Como incidentes evoluem no tempo?" via
  `POST /api/operations/incidentes/stats/timeseries` com `field=ocorridoEm`;
- "Quais ameacas estao ativas, em confronto, observacao ou capturadas?" via
  `POST /api/risk-intelligence/ameacas/stats/group-by` com `field=status`;
- "Quais comandos reais de triagem existem para ameacas?" via
  `/schemas/actions?resource=risk-intelligence.ameacas`, incluindo
  `mark-under-observation` e `mark-captured`;
- "A leitura analitica bate com a ocorrencia transacional?" via
  `POST /api/risk-intelligence/vw-indicadores-incidentes/stats/group-by` com
  `field=severidade`.

O smoke `scripts/verify-risk-intelligence-runtime.sh` protege essas evidencias
no host publicado e confirma as surfaces `threat-monitoring-board` e
`incident-investigation-board`, alem das actions reais de triagem de ameacas.

### Relacionamentos navegaveis

Fonte no Quickstart:

- campos de DTO com referencias;
- option sources;
- request/response schemas;
- surfaces e actions que apontam recursos dependentes;
- ratios calculados pelo cockpit a partir de campos, endpoints e capabilities.

Materializacao esperada:

- diagrama navegavel entre recursos;
- destaque do recurso selecionado;
- arestas com motivo compreensivel: lookup, composicao, action, stats ou detalhe;
- preservacao do dominio selecionado para manter contexto visual.

## Prioridades recomendadas de melhoria no Quickstart

1. `risk-intelligence`: ameacas ja publicam monitoramento, stats e actions reais
   de triagem; incidentes ja existem em `operations.incidentes` como recurso
   transacional com surface/stats de investigacao. A proxima evolucao correta
   e modelar comandos reais de ciclo de vida de incidente apenas quando houver
   regra de negocio canonica para triagem, escalonamento ou encerramento,
   preservando `vw-indicadores-incidentes` como leitura analitica.
2. `human-resources`: o cockpit agora materializa disponibilidade por
   ferias/afastamentos e ranking reputacional como leitura executiva. A proxima
   expansao deve ser um comando real de ciclo de vida, como aprovacao de ausencia
   ou revisao reputacional, apenas quando houver estado persistido e regra de
   negocio canonica para sustentar a action.
3. `procurement`: ja publica charts de compras, uma surface de jornada
   governada em `procurement.purchase-orders` e relacionamentos navegaveis
   fornecedor -> contratos/pedidos e contrato -> produtos/pedidos; a migration operacional
   `V20260703_003__procurement_cockpit_lifecycle_seed.sql` reidrata o ciclo
   aprovado/cancelado/recebido dos pedidos demonstrativos para que os charts
   evidenciem gargalos, fornecedores bloqueados, contratos vencendo e pedidos
   em risco tambem em bancos ja existentes. O smoke
   `scripts/verify-procurement-analytics-runtime.sh` protege essa evidencia no
   host publicado.
4. `assets`: ja publica relacionamentos navegaveis equipamento -> custodias e
   veiculo -> usos em missao; a proxima evolucao deve enriquecer amostras de
   dados para evidenciar perdas, danos, manutencoes e devolucoes em volume
   suficiente para o cockpit demonstrar outliers e comparativos reais.
5. `operations`: manter como benchmark visual e documental para os demais dominios.
   O smoke `scripts/verify-operations-runtime.sh` agora protege a evidencia publica
   de workflows, surfaces, charts e relacionamentos; a proxima evolucao deve ser
   novo comportamento real de negocio, nao actions copiadas por simetria.

Essas prioridades sao melhoria de host exemplar, nao lacuna de contrato. Se, ao
implementar uma delas, faltar capacidade no starter para descrever uma action,
relationship ou chart, ai sim a lacuna deve ser registrada contra o starter como
`lacuna-real-de-contrato`.

## Checklist para novo recurso exemplar

Antes de considerar um recurso "pronto para cockpit", valide:

- `@ApiResource(resourceKey=...)` estavel e orientado ao dominio.
- `@Operation` em portugues claro, explicando finalidade de negocio.
- DTOs e filtros com `@Schema` de negocio, nao apenas label tecnico.
- `@UISchema` separado da semantica de negocio.
- Option sources para selecoes relevantes.
- Stats quando o recurso sustenta dashboard ou comparacao.
- `@UiSurface` quando existir experiencia composta materializavel.
- `@WorkflowAction` quando existir comando de negocio materializavel.
- Evidencia em `/schemas/catalog`, `/v3/api-docs/<grupo>` e cockpit.
- Schemas filtrados de request/response para cada action em `/schemas/filtered`.
- Teste focal quando a mudanca alterar comportamento publicado.

## Validacao rapida

Com o host publicado:

```bash
curl -fsS https://praxis-api-quickstart.onrender.com/actuator/info | jq '.build'
curl -fsS https://praxis-api-quickstart.onrender.com/schemas/catalog | jq '.endpoints | length'
curl -fsS https://praxis-api-quickstart.onrender.com/v3/api-docs/operations | jq '.paths | keys | length'
```

Com o projeto local:

```bash
rg -l '@ApiResource' src/main/java | wc -l
rg -n '@UiSurface' src/main/java | wc -l
rg -n '@WorkflowAction' src/main/java | wc -l
```

Para mudancas apenas documentais, a validacao minima e leitura final dos arquivos
alterados. Para mudancas em controllers, DTOs ou annotations publicadas, use os
testes focais definidos no `AGENTS.md` local.
