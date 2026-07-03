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
| Suprimentos | `procurement` | 5 | 4 | 0 | `suportado-parcialmente`: recursos, schemas, option sources e surfaces de compra/contrato/fornecedor existem; falta action de workflow somente quando houver comando real de negocio. |
| Ativos Operacionais | `assets` | 4 | 0 | 0 | `suportado-parcialmente`: recursos conectam equipamentos, veiculos, missoes e alocacoes; falta surface/action exemplar para movimentacao e alocacao. |
| Inteligencia de Risco | `risk-intelligence` | 2 | 0 | 0 | `suportado-parcialmente`: bom potencial analitico; falta surface de painel/serie e action de triagem ou revisao de ameaca. |

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

### Charts e analytics

Fonte no Quickstart:

- views analiticas;
- endpoints `stats/*`;
- descricoes de `@Operation` que expliquem dimensoes, metricas e serie temporal.

Recursos atuais com maior potencial:

- `human-resources.vw-analytics-folha-pagamento`;
- `operations.vw-resumo-missoes`;
- `risk-intelligence.vw-indicadores-incidentes`.

O cockpit deve preferir explicar charts por pergunta de negocio, por exemplo:
"Como a severidade de incidentes evolui por base?" em vez de apenas listar que
um endpoint `stats` existe.

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

1. `procurement`: evoluir de surfaces declarativas para actions de negocio como
   revisar contrato, aprovar pedido ou bloquear fornecedor apenas quando houver
   endpoint/service real que execute o comando governado.
2. `assets`: publicar surface de alocacao operacional conectando equipamento,
   veiculo e missao; adicionar action de reservar, liberar ou transferir ativo
   quando o service ja suportar comando claro.
3. `risk-intelligence`: publicar surface analitica para incidentes/ameacas e
   action de revisar severidade, marcar ameaca monitorada ou abrir investigacao.
4. `human-resources`: ampliar exemplo de ciclo de vida alem de folha, como
   afastamento, reputacao ou habilidade, se houver comando real de dominio.
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
