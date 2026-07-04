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
| Pessoas e RH | `human-resources` | 20 | 6 | 4 | `suportado-parcialmente`: boa documentacao de campos, actions em folha e surfaces de funcionarios, mas ainda pode ganhar mais exemplos de workflows de ciclo de vida. |
| Operacoes | `operations` | 12 | 9 | 10 | `ja-suportado-mal-nomeado-ou-mal-materializado`: e o melhor dominio para demonstrar actions/surfaces; cockpit deve usar esse dominio como referencia visual de workflows. |
| Suprimentos | `procurement` | 5 | 4 | 8 | `ja-suportado-mal-nomeado-ou-mal-materializado`: recursos, schemas, option sources, surfaces e actions de fornecedor, contrato e pedido existem; o cockpit deve materializar a jornada compra -> contrato -> pedido -> recebimento como fluxo navegavel. |
| Ativos Operacionais | `assets` | 4 | 4 | 7 | `ja-suportado-mal-nomeado-ou-mal-materializado`: recursos, lookups, surfaces, actions de disponibilidade e actions de custodia existem; o cockpit deve materializar inventario -> custodia -> devolucao/perda/dano como fluxo navegavel. |
| Inteligencia de Risco | `risk-intelligence` | 2 | 3 | 2 | `suportado-parcialmente`: recurso de ameacas e view analitica de incidentes publicam surfaces de monitoramento, painel, chart e actions reais de triagem; ainda pode evoluir comandos sobre incidentes quando houver recurso transacional dedicado. |

Nenhum item acima exige contrato novo neste momento. A plataforma ja sabe expor
`@ApiResource`, `@Operation`, `@Schema`, `@UISchema`, stats, options, surfaces,
actions e capabilities. A lacuna principal e de exemplaridade no host: alguns
dominios ainda nao publicam annotations suficientes para o cockpit revelar todo
o potencial da plataforma.

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
- `procurement.companies`: surface de escopo de empresas compradoras;
- `procurement.suppliers`: surface de homologacao, risco e elegibilidade;
- `procurement.contracts`: surface de governanca contratual;
- `procurement.products`: surface de catalogo de produtos contratados;
- `procurement.purchase-orders`: surface de jornada governada empresa -> fornecedor -> contrato -> produto -> pedido, com actions de aprovar, cancelar e receber pedido;
- `assets.equipamento-alocacoes`: surface de cadeia de custodia e actions de devolucao, perda e dano;
- `human-resources.funcionarios`: surfaces de perfil e gestao de pessoas;
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

1. `risk-intelligence`: evoluir actions de ameacas para comandos sobre
   incidentes quando houver recurso transacional dedicado, preservando a view
   `vw-indicadores-incidentes` como leitura analitica.
2. `human-resources`: ampliar exemplo de ciclo de vida alem de folha, como
   afastamento, reputacao ou habilidade, se houver comando real de dominio.
3. `procurement`: ja publica charts de compras e uma surface de jornada
   governada em `procurement.purchase-orders`; a migration operacional
   `V20260703_003__procurement_cockpit_lifecycle_seed.sql` reidrata o ciclo
   aprovado/cancelado/recebido dos pedidos demonstrativos para que os charts
   evidenciem gargalos, fornecedores bloqueados, contratos vencendo e pedidos
   em risco tambem em bancos ja existentes. O smoke
   `scripts/verify-procurement-analytics-runtime.sh` protege essa evidencia no
   host publicado.
4. `assets`: evoluir amostras de dados para evidenciar perdas, danos,
   manutencoes e devolucoes em volume suficiente para o cockpit demonstrar
   outliers e comparativos reais.
5. `operations`: usar como benchmark visual e documental para os demais dominios,
   evitando copiar actions sem comportamento real.

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
