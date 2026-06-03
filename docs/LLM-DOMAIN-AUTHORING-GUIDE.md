# Guia de autoria de dominio para LLMs e analistas

Este guia descreve como um analista de negocio e uma LLM devem explicar, propor, revisar e aplicar rascunhos de regras usando o catalogo semantico da Praxis em runtime.

A premissa central e simples: a LLM nao deve precisar ler codigo-fonte para entender o dominio. Ela deve operar sobre vocabulario, relacionamentos, evidencias e governanca publicados pelo proprio sistema.

No host de referencia, essa governanca tambem ficou explicitamente declarada no
codigo-fonte por meio de `@DomainGovernance` e `@AiUsagePolicy`, para que o
exemplo seja self-describing sem depender apenas de heuristicas do catalogo.

Para novos hosts, use tambem o guia [`AI-HOST-BUSINESS-GROUNDING-GUIDE.md`](AI-HOST-BUSINESS-GROUNDING-GUIDE.md).
Ele transforma o quickstart em checklist operacional: como anotar recursos,
operacoes, DTOs, governanca, stats, option sources e conhecimento adicional para
que a IA aprenda qualquer negocio pelo runtime Praxis, sem herdar heuristicas
especificas de RH, folha, procurement ou missoes.

## O que esta camada e

O catalogo semantico e a primeira camada de conhecimento do dominio. Ele responde perguntas como:

- quais conceitos existem no dominio;
- quais campos representam dados pessoais, financeiros, regulatorios ou operacionais;
- quais recursos se relacionam;
- quais evidencias explicam a origem da interpretacao;
- quais restricoes de uso por IA existem para cada dado.

Ele prepara a autoria de regras, mas ainda nao executa regras.

## O que esta camada nao e

O catalogo semantico nao deve virar um motor de regras disfarcado. Ele nao substitui:

- validacao transacional no backend;
- autorizacao RBAC/ABAC;
- politicas OPA/Rego;
- regras materializadas no `FormConfig`;
- workflow de aprovacao humana;
- auditoria formal de mudanca de regra.

Quando a LLM sugerir uma regra, a resposta deve deixar claro que a regra e um rascunho ate ser revisada, aprovada e materializada na camada correta.

No runtime atual, a separacao esperada e:

- `domain_rule_definition`: definicao compartilhavel, versionada, com ownership, condicao, parametros, governanca e evidencia;
- `domain_rule_simulation`: preview deterministico da decisao proposta, com grounding, cobertura existente, materializacoes previstas e aprovacoes requeridas;
- `domain_rule_materialization`: copia aplicada em um alvo concreto, como `FormConfig.formRules[]`, backend, frontend ou workflow;
- `FormConfig`: estado materializado para execucao/preview do formulario, nunca a fonte primaria da regra compartilhavel.

No corte atual do `praxis-config-starter`, o fluxo canonico em evolucao passa a ser:

- `POST /api/praxis/config/domain-rules/intake` para abrir um draft governado a partir do pedido em linguagem natural;
- `POST /api/praxis/config/domain-rules/simulations` para prever o impacto da decisao;
- `POST /api/praxis/config/domain-rules/publications` para promover uma definicao persistida quando `publicationReadiness=ready_to_publish`;
- `POST /api/praxis/config/domain-rules/definitions` para persistir a definicao governada;
- `POST /api/praxis/config/domain-rules/materializations` para projetar a definicao em um target concreto.

No host operacional de referencia, o smoke canônico agora prova dois caminhos
complementares:

- `human-resources.funcionarios -> visual_guidance -> form_config`, mantendo a
  trilha de LGPD/explicabilidade no formulario;
- `procurement.suppliers -> selection_eligibility -> option_source`, usando
  `publications` para derivar e aplicar a policy de selecao canônica sem patch
  local do host, com tenant de publicacao derivado de `SMOKE_RUN_ID` para
  evitar colisao entre execucoes. Apos a publicacao, o smoke consulta o lookup
  real de fornecedores com esse tenant para provar que a policy aplicada pelo
  `domain-rules` governa o runtime de `option_source`.
- `procurement.purchase-orders -> validation/compliance -> backend_validation`,
  como hook de runtime no host: o servico de pedidos consulta materializacoes
  aplicadas de `resource_validation_policy` e bloqueia comandos com `supplierId`
  apontando para fornecedor em status proibido. A autoria e publicacao
  continuam canônicas no `praxis-config-starter`; o quickstart apenas consome a
  decisao aplicada para provar enforcement transacional.
- `human-resources.folhas-pagamento -> workflow_action_policy -> workflow_action`,
  como hook de action operacional: o servico de folha consulta materializacoes
  aplicadas de `workflow_action_policy` para
  `human-resources.folhas-pagamento:mark-paid` e bloqueia a action com `409`
  quando a decisao governada se aplica ao estado atual.
- `human-resources.eventos-folha -> approval_policy -> approval_policy`,
  como proxima materializacao governada: a regra canonica decide se uma action
  existente exige aprovacao, qual contexto semantico de aprovador se aplica e
  como o runtime deve explicar o bloqueio, sem criar inbox generico ou motor BPM.
- `human-resources.funcionarios -> domain-knowledge/change-sets -> add_evidence`,
  como primeiro fluxo de escrita governada de conhecimento: o smoke
  `scripts/verify-domain-knowledge-change-set-runtime.sh` garante contexto
  semantico persistido, cria uma proposta LLM segura, revalida a proposta,
  aprova com revisor explicito, aplica somente `add_evidence` contra conceito
  existente e le o change set aplicado sem expor patch cru como fonte primaria.
  Quando o runtime expuser
  `GET /api/praxis/config/domain-knowledge/change-sets/{id}/timeline`, o smoke
  tambem valida a timeline segura de criacao, validacao, aprovacao e aplicacao;
  use `REQUIRE_CHANGE_SET_TIMELINE=true` para tornar essa auditoria obrigatoria
  em cortes que ja incluem essa superficie. Para fases que tambem precisam
  provar reversibilidade governada, adicione `REQUIRE_EVIDENCE_REVERT=true`: o
  mesmo smoke cria um segundo change set com `revert_evidence`, revalida,
  aprova, aplica e exige timeline segura com `evidence.reverted`, sem delete
  fisico nem exposicao de payload bruto. Para provar supersession governada,
  adicione `REQUIRE_EVIDENCE_SUPERSESSION=true`: o smoke cria evidencia
  substituta ativa no mesmo change set de add, aplica `revert_evidence` com
  `replacementEvidenceKey` e exige timeline segura com `evidence.superseded`,
  sem `evidence.reverted` duplicado. Para provar retirada de influencia em
  authoring, use `REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL=true`: o smoke prepara um
  conceito de Project Knowledge, confirma que ele entra no stream apos
  `add_evidence` e confirma que deixa de aparecer depois de revert puro; quando
  combinado com `REQUIRE_EVIDENCE_SUPERSESSION=true`, confirma que a substituta
  ativa mantem o conceito recuperavel. Para provar o caminho vector-enabled
  local-first, use `REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL=true` com o
  backend iniciado com `PRAXIS_PROJECT_KNOWLEDGE_RAG_PUBLICATION_ENABLED=true`,
  `PRAXIS_PROJECT_KNOWLEDGE_RAG_RETRIEVAL_ENABLED=true` e vector store
  habilitado; o smoke exige o documento derivado `project_knowledge` no
  `vector_store` apos `add_evidence` e valida remocao/retencao no lifecycle de
  `revert_evidence` e supersession.

O mesmo corte tambem deve ser validado pela UI oficial quando o objetivo for
provar o cockpit governado do Page Builder. Nesse caso, use o runner local em
`../praxis-ui-angular/tools/local-e2e/run-project-knowledge-audit-cockpit-local.sh`:
ele sobe quickstart e Angular em portas isoladas, injeta uma auditoria segura
de Project Knowledge, cria o change set em
`/api/praxis/config/domain-knowledge/change-sets`, valida, aprova, aplica e le
o readback sem expor `conceptKey`, `sourceSummary`, payload bruto ou resumo de
conhecimento na UI. Semanticas como `project_preference` continuam sendo
metadados de origem; o `evidenceType` persistido no caminho do Page Builder
deve permanecer canonico, como `llm_proposal`.

Checkpoint local da UI em 2026-05-01:

```bash
cd ../praxis-ui-angular

AI_PROVIDER=openai \
AI_ENV_FILE=../praxis-config-starter/.env.openai.local.sh \
PRAXIS_E2E_TIMEOUT_MS=900000 \
./tools/local-e2e/run-project-knowledge-audit-cockpit-local.sh
```

Resultado observado: `1 passed (10.2s)` apos quickstart e Angular ficarem
prontos. A prova tambem validou o corpo da timeline segura no cockpit, sem
renderizar `conceptKey`, `sourceSummary`, `sourcePointer`, `patchHash`,
`assistantMessage`, `materializedPayload` ou resumo bruto de conhecimento.
Essa validacao continua local-first; GitHub Actions deve ficar reservado para
fechamento de fase, release ou smoke publicado.

A timeline de change set e uma superficie de auditoria segura: ela deve expor
somente eventos, status, validacao, tipos de operacao e alvos seguros. Ela nao
deve retornar patch bruto, payload de evidencia, `sourcePointer`, `sourceUri`,
`patchHash`, prompt ou historico de chat.

Na resposta de `simulation`, a explicacao canônica do impacto deve vir do
backend em `explainability`, junto de `grounding`, `existingCoverage`,
`predictedMaterializations`, `requiredApprovals` e `warnings`. O host nao deve
reconstruir esse resumo localmente como se fosse uma segunda fonte de verdade.

Enquanto o runtime publicado ainda estiver em transicao para esse fluxo, a ausencia de `/simulations` deve ser tratada como gap de rollout do host, nao como motivo para voltar a modelar regra de negocio via `componentEditPlan`.

## Contexto minimo para uma solicitacao

Toda tarefa enviada para a LLM deve informar, quando possivel:

- `serviceKey`: servico dono do contexto, por exemplo `praxis-service`;
- `resourceKey`: recurso principal, por exemplo `human-resources.funcionarios`;
- objetivo de negocio;
- tipo de artefato desejado, como `form-rule`, `validation-draft`, `ui-policy`, `dashboard`, `workflow-hint`, `workflow-action-policy` ou `approval-policy`;
- publico alvo da resposta, como analista, desenvolvedor, auditor ou operador;
- ambiente e tenant, quando a decisao depender de escopo;
- consulta de recuperacao, como `cpf`, `salario`, `status` ou uma frase de negocio.

Antes de responder, a LLM deve recuperar contexto por uma destas superficies:

```bash
GET /schemas/domain?resourceKey=<resourceKey>
```

```bash
GET /api/praxis/config/domain-catalog/context?serviceKey=<serviceKey>&resourceKey=<resourceKey>&q=<consulta>
```

Quando a tarefa passar pelo authoring HTTP, a UI ou o agente deve preservar esses dados em `contextHints.domainCatalog`. O contrato versionavel esta em [`contracts/domain-authoring-context-hints.schema.json`](contracts/domain-authoring-context-hints.schema.json), com exemplo em [`../payloads/domain_authoring_context_hints.example.json`](../payloads/domain_authoring_context_hints.example.json). Esse arquivo do quickstart espelha o contrato canonico `AiDomainCatalogContextHint` do `praxis-config-starter` e so acrescenta extensoes opcionais de autoria direta, como `artifactKind`, `targetLayer`, `recommendedAuthoringFlow`, `recommendedRuleType` e `governance`. O host de referencia valida esse contrato nos testes `DomainAuthoringContextHintsContractTest` e `AiPatchSchemaResolutionIsolatedIntegrationTest`, alem do smoke runtime `scripts/verify-domain-catalog-authoring-runtime.sh`. Os campos minimos sao `serviceKey`, `resourceKey`, `type`, `query` e `limit`; `schemaVersion`, `intent` e `itemTypes` devem ser usados quando a chamada precisar deixar claro a versao e a finalidade da recuperacao semantica.

## Contrato de resposta esperado

Para tarefas de autoria, a LLM deve responder em uma estrutura previsivel:

```json
{
  "intent": "criar regra de exibicao para campo sensivel",
  "resourceKey": "human-resources.funcionarios",
  "catalogRelease": "praxis-service:human-resources.funcionarios:...",
  "domainUnderstanding": ["Funcionario", "CPF", "dado pessoal"],
  "evidence": ["binding/schema/campo cpf", "governance/cpf"],
  "governanceFindings": [
    {
      "field": "cpf",
      "classification": "confidential",
      "complianceTags": ["LGPD", "GDPR"],
      "visibility": "mask",
      "trainingUse": "deny",
      "ruleAuthoring": "review_required"
    }
  ],
  "proposedDraft": {
    "type": "ui-policy",
    "recommendedAuthoringFlow": "shared_rule_authoring",
    "summary": "Exibir CPF mascarado por padrao e exigir aprovacao para uso em regra automatica.",
    "definitionTarget": "domain_rule_definition",
    "materializationTarget": "domain_rule_materialization:form_config"
  },
  "requiredApprovals": ["data-owner", "security-review"],
  "targetLayer": "future shared rule layer or FormConfig materialization",
  "nonExecutableWarnings": [
    "Catalogo semantico nao executa autorizacao.",
    "Rascunho nao deve ser aplicado sem aprovacao humana."
  ]
}
```

A forma exata pode evoluir, mas esses blocos devem existir conceitualmente: intencao, contexto, evidencias, governanca, proposta, aprovacao e camada de materializacao.

Para decisoes que governam a execucao de uma acao de recurso ja exposta pelo
host, como aprovar eventos ou marcar uma folha como paga, use
`artifactKind=workflow-action-policy` e `targetLayer=workflow_action`. Esse alvo
representa uma materializacao derivada para uma acao operacional existente, nao
um motor de workflow paralelo. A decisao canonica permanece em
`domain_rule_definition`; o runtime consome a projecao como
`targetArtifactType=resource-workflow-action`.

Para decisoes que governam a necessidade de aprovacao antes de executar uma
acao de recurso ja exposta, use `artifactKind=approval-policy` e
`targetLayer=approval_policy`. Esse alvo representa uma materializacao derivada
para o gate de aprovacao da action, nao uma fila generica de aprovacoes ou um
motor BPM. A decisao canonica permanece em `domain_rule_definition`; o runtime
consome a projecao como `targetArtifactType=resource-action-approval`.

## Exemplos realistas

### Explicar CPF para um analista

Prompt:

```text
Explique como o campo CPF deve ser tratado em uma tela de funcionarios.
Use somente o catalogo semantico em runtime e cite os achados de governanca.
```

Resposta esperada:

- identificar `human-resources.funcionarios`;
- explicar que `cpf` e dado pessoal sensivel para fins de privacidade;
- citar LGPD/GDPR quando presentes em `complianceTags`;
- aplicar `visibility=mask`;
- negar uso para treino quando `trainingUse=deny`;
- marcar autoria de regra como dependente de revisao quando `ruleAuthoring=review_required`.

### Propor uma regra de UI para salario

Prompt:

```text
Crie um rascunho de regra para dashboards de folha que mostrem salario.
Quero saber o que pode ser exibido automaticamente e o que exige revisao.
```

Resposta esperada:

- recuperar `human-resources.folhas-pagamento`;
- tratar salario como dado financeiro e possivelmente pessoal;
- propor exibicao agregada ou mascarada quando a governanca indicar restricao;
- explicitar que permissao de usuario nao nasce no catalogo semantico;
- recomendar materializacao futura em politica de UI, autorizacao ou regra compartilhavel.

### Melhorar a tela de missoes

Prompt:

```text
Proponha uma melhoria para a tela de missoes.
Quero destacar missoes atrasadas, mostrar participantes e eventos recentes.
Use somente o contexto semantico de runtime.
```

Resposta esperada:

- recuperar `operations.missoes`;
- usar status, datas e prioridade como candidatos de destaque;
- seguir `edges` para participantes e eventos;
- nao tratar a missao como tabela plana;
- consultar governanca antes de sugerir automacao com dados sensiveis.

### Criar uma nova categorizacao LGPD

Prompt:

```text
Adicione uma proposta de categorizacao LGPD para um novo campo de funcionario.
Explique que anotacoes ou metadados precisam existir para a LLM entender isso em runtime.
```

Resposta esperada:

- pedir nome semantico, descricao, categoria de dado e finalidade;
- exigir `classification`, `dataCategory`, `complianceTags` e `aiUsage`;
- indicar bindings para schema/API/UI;
- marcar a proposta como rascunho ate revisao do data-owner;
- evitar alterar regra executavel sem uma camada de aprovacao.

### Criar orientacao visual de dominio no Dynamic Form

Prompt:

```text
Crie uma sugestao de orientacao visual para avisar o analista quando CPF ou salario
estiverem envolvidos. Use apenas o catalogo semantico em runtime e nao escreva
formRulesState.
```

Resposta esperada:

- recuperar governanca para `human-resources.funcionarios` ou `human-resources.folhas-pagamento`;
- citar evidencias de privacidade, financeiro, LGPD/GDPR ou politica interna;
- propor rascunho com `artifactKind=form-rule` e `targetLayer=form-config-materialization`;
- usar `recommendedAuthoringFlow=shared_rule_authoring` quando o pedido for uma regra compartilhavel governada, mesmo que depois exista materializacao derivada em `visualBlock`;
- tratar `rule.visualBlockGuidance.add` somente como operacao de materializacao visual derivada, nunca como recomendacao primaria de autoria da decisao;
- simular primeiro o rascunho em `/api/praxis/config/domain-rules/simulations` para validar grounding, aprovacoes e materializacoes previstas;
- usar `/api/praxis/config/domain-rules/publications` quando a definicao persistida ja estiver com `publicationReadiness=ready_to_publish`, em vez de remontar localmente a policy de ativacao/aplicacao;
- criar primeiro uma definicao compartilhavel em `/api/praxis/config/domain-rules/definitions`;
- criar depois a materializacao `form_config` em `/api/praxis/config/domain-rules/materializations`;
- exigir `metadata.origin="llm"` e `metadata.reviewStatus="pending"`;
- lembrar que `formRulesState` e materializado apenas pelo editor apos revisao humana.

## Regras de seguranca para a LLM

A LLM deve:

- usar apenas contexto publicado em runtime para interpretar o dominio;
- separar explicacao semantica de regra executavel;
- citar evidencias ou informar quando elas nao existem;
- respeitar `aiUsage.visibility`, `trainingUse` e `ruleAuthoring`;
- mascarar dados quando a governanca pedir;
- declarar hipoteses e lacunas em vez de inventar regra;
- pedir revisao humana quando houver privacidade, compliance, financeiro ou seguranca.

A LLM nao deve:

- ler codigo-fonte para descobrir regra de negocio em fluxos de runtime;
- promover rascunho diretamente para producao;
- assumir autorizacao a partir de nomes de campos;
- usar dado com `trainingUse=deny` para treino, memoria ou exemplo;
- misturar decisao OPA/Rego com vocabulario semantico;
- criar regra compartilhavel sem ownership, versao, evidencia e aprovacao.

## Ciclo recomendado de autoria

1. Descobrir o recurso e a release do catalogo.
2. Recuperar contexto por consulta de negocio.
3. Explicar vocabulario, relacionamentos e governanca.
4. Gerar rascunho de regra ou melhoria.
5. Validar evidencias e restricoes de IA.
6. Solicitar aprovacao humana quando necessario.
7. Simular a decisao compartilhavel para validar grounding, cobertura existente, aprovacoes e targets previstos.
8. Persistir a definicao compartilhavel quando houver ownership, governanca e evidencia suficientes.
9. Materializar na camada correta: backend, frontend, `FormConfig` ou workflow.
10. Validar o contrato runtime com `scripts/verify-domain-rules-runtime.sh` quando o alvo envolver regra compartilhavel e suas materializacoes derivadas.
11. Auditar a origem, versao e decisao tomada.

## Como isso guia a arquitetura futura

A proxima camada de regra compartilhavel deve depender deste catalogo, nao substitui-lo.

O desenho recomendado e:

- catalogo semantico: vocabulario, relacionamento, evidencia e governanca;
- regra compartilhavel: definicao versionada, ownership, parametros e condicoes;
- regra materializada: copia aplicada em `FormConfig`, backend, frontend ou workflow;
- politica de decisao: autorizacao, ABAC/RBAC ou OPA/Rego quando houver decisao formal;
- auditoria: quem pediu, quem aprovou, qual release e qual evidencia sustentaram a mudanca.

Assim a Praxis fica preparada para sistemas escritos, explicados e alterados por IAs sem perder rastreabilidade, governanca e controle humano.
