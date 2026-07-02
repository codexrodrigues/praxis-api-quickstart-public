# Contrato operacional do catálogo semântico

Este documento fecha a linha operacional originalmente validada em 2026-04-22 para a fundação de vocabulário e governança de domínio da Praxis, com versões alinhadas ao ciclo corrente do host de referência.

## Objetivo

O contrato do catálogo semântico permite que humanos, frontends, backends e LLMs leiam o mesmo mapa de domínio em runtime, sem obrigar a LLM a inferir negócio a partir do código-fonte.

Ele não substitui regras executáveis, autorização, validação transacional ou política externa. A primeira camada é vocabulário, evidência, governança e contexto recuperável.

A redação de **definições de negócio** em DTOs (`@Schema`) no host — em particular a separação entre texto de **contrato** e rótulos de **UI** (`@UISchema`) — segue a secção *Documentação de domínio em DTOs e OpenAPI* em `AGENTS.md` deste projeto, de forma a alinhar OpenAPI e catálogo com o mesmo mapa semântico sem colapsar significado em etiquetas de ecrã.

## Versões alinhadas

- `praxis-metadata-starter`: `8.0.0-rc.37`
- `praxis-config-starter`: `0.1.0-rc.71`
- `praxis-ui-angular`: `9.0.0-beta.9`
- `praxis-api-quickstart`: host operacional de referência usando os dois starters publicados no Maven Central.

## Responsabilidades

`praxis-metadata-starter`:
- publica `GET /schemas/domain`;
- emite `schemaVersion=praxis.domain-catalog/v0.2`;
- materializa contextos, nós, edges, bindings, evidências, aliases e governança;
- aceita governança declarada diretamente no código-fonte com
  `@DomainGovernance` e `@AiUsagePolicy`, publicada em runtime antes do fallback
  heurístico;
- mantém os valores compatíveis com o contrato do config-starter, incluindo `annotationType`, `dataCategory` e `aiUsage.visibility`.

`praxis-config-starter`:
- valida o payload com JSON Schema antes de persistir;
- persiste releases em `domain_catalog_release`;
- persiste itens em `domain_catalog_item`;
- publica consulta transacional por release em `/api/praxis/config/domain-catalog/items`;
- publica contexto LLM em `/api/praxis/config/domain-catalog/context`;
- persiste definicoes versionadas de regra em `domain_rule_definition`;
- persiste materializacoes por alvo em `domain_rule_materialization`;
- publica APIs de regra compartilhavel em `/api/praxis/config/domain-rules/**`;
- publica APIs de federacao de dominio em `/api/praxis/config/domain-federation/**`;
- permite rodar persistência/projeção do catálogo semântico sem RAG/vector store;
- só cria `VectorStore` quando `praxis.ai.rag.vector-store.enabled=true`;
- mantém `RagChatAdvisorProperties` e `RagChatAdvisorService` disponíveis mesmo quando o vector store está desligado.

`praxis-api-quickstart`:
- prova a compatibilidade real entre os starters publicados;
- cobre emissão `/schemas/domain` em teste isolado;
- cobre ingestão runtime contra config-store remoto quando executado localmente;
- cobre ausência real de `VectorStore` quando `praxis.ai.rag.vector-store.enabled=false`;
- cobre política de isolamento dos testes Spring para não herdarem datasources remotos na suíte padrão;
- oferece scripts para ingestão e verificação semântica.

## Superfícies runtime

- `GET /schemas/domain?resourceKey=<resourceKey>`: catálogo emitido pelo host.
- `POST /api/praxis/config/domain-catalog/ingest`: persistência da release.
- `GET /api/praxis/config/domain-catalog/releases`: descoberta das releases persistidas.
- `GET /api/praxis/config/domain-catalog/items?releaseKey=...`: consulta determinística por release.
- `GET /api/praxis/config/domain-catalog/context`: contexto mais recente para prompt/LLM; informe `resourceKey` quando o mesmo `serviceKey` publicar varios recursos.
- `POST /api/praxis/config/domain-rules/intake`: abre um draft governado de regra compartilhavel a partir do pedido em linguagem natural e devolve grounding para o proximo passo canonico.
- `POST /api/praxis/config/domain-rules/definitions`: cria uma definicao versionada e governada de regra compartilhavel.
- `GET /api/praxis/config/domain-rules/definitions`: consulta definicoes por filtros como `resourceKey`, `ruleType`, `status` e `ruleKey`.
- `PATCH /api/praxis/config/domain-rules/definitions/{definitionId}/status`: promove ou rejeita uma definicao pela trilha governada, sem executar regra no host.
- `POST /api/praxis/config/domain-rules/simulations`: simula uma decisao compartilhavel antes da persistencia, retornando grounding, cobertura existente, materializacoes previstas, aprovacoes requeridas, warnings e `explainability` estruturada.
- `POST /api/praxis/config/domain-rules/publications`: promove uma definicao persistida quando `publicationReadiness=ready_to_publish`, ativando a regra e aplicando materializacoes elegiveis de forma governada.
- `POST /api/praxis/config/domain-rules/materializations`: cria uma materializacao de regra para um alvo concreto, como `FormConfig.formRules[]`.
- `GET /api/praxis/config/domain-rules/materializations`: consulta materializacoes por alvo, status e definicao.
- `PATCH /api/praxis/config/domain-rules/materializations/{materializationId}/status`: governa o ciclo de vida da projecao derivada, como `pending_review -> applied`.
- `POST /api/praxis/config/domain-federation/ingest?dryRun=true|false`: valida e, quando habilitado, persiste uma release federada candidata.
- `GET /api/praxis/config/domain-federation/releases`: lista releases federadas por escopo/status.
- `GET /api/praxis/config/domain-federation/releases/{releaseKey}/validation`: consulta o relatorio de validacao persistido.
- `POST /api/praxis/config/domain-federation/releases/{releaseKey}/activate`: promove uma release candidata para ativa no escopo `tenantId` + `environment`.

Para gates determinísticos por release, prefira `/releases` + `/items?releaseKey=...`. Para recuperação LLM por recurso, use `/context` com `serviceKey` e `resourceKey`; sem `resourceKey`, o endpoint continua retornando a release mais recente do serviço para recuperação ampla.

## Recursos críticos do gate atual

- `human-resources.funcionarios`
- `human-resources.folhas-pagamento`
- `operations.missoes`
- `operations.acordos-regulatorios`
- `procurement.suppliers`

Consultas representativas:
- `cpf` para funcionários;
- `salario` para folha;
- `status` para missões, acordos regulatórios e fornecedores.

## Gates obrigatórios

CI local/isolado:
- `./mvnw -Dtest=OpenApiGroupResolutionIsolatedIntegrationTest,AiPatchSchemaResolutionIsolatedIntegrationTest test`
- `bash -n scripts/*.sh`

CI remoto:
- workflow `CI (Java)`;
- passo `Validate shell scripts`;
- passo `Maven Verify (com testes)`.
- workflow `Domain Catalog Runtime Smoke`, manual e agendado, contra runtime
  publicado.
- workflow `Domain Rules Runtime Smoke`, manual, contra runtime publicado com
  `V20` aplicado.
- smoke local `scripts/verify-domain-federation-runtime.sh`, contra runtime
  com `V21` aplicado e persistencia de federacao habilitada.

Runtime remoto, sem migrations:

```bash
PORT=8088 \
SPRING_FLYWAY_ENABLED=false \
PRAXIS_DOMAIN_KNOWLEDGE_PROJECTION_ENABLED=true \
PRAXIS_DOMAIN_CATALOG_RAG_PUBLICATION_ENABLED=false \
PRAXIS_AI_RAG_VECTOR_STORE_ENABLED=false \
PRAXIS_AI_REGISTRY_BOOTSTRAP_ENABLED=false \
PRAXIS_AI_REGISTRY_HEALTH_ENABLED=false \
SPRING_AI_VECTORSTORE_PGVECTOR_INITIALIZE_SCHEMA=false \
SPRING_AI_VECTORSTORE_PGVECTOR_VECTOR_TABLE_VALIDATIONS_ENABLED=false \
./mvnw spring-boot:run
```

Esse modo valida a camada transacional e a projeção `domain_knowledge_*` sem publicar documentos RAG, sem acionar embeddings e sem inicializar/validar pgvector.

Ingestão:

```bash
BACKEND_URL=http://localhost:8088 scripts/ensure-domain-catalog-context.sh
```

Verificação somente leitura:

```bash
BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-context.sh
```

Verificação runtime do contexto LLM e envelope de autoria:

```bash
BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-authoring-runtime.sh
```

Verificação runtime da camada de regra compartilhavel:

```bash
BACKEND_URL=http://localhost:8088 scripts/verify-domain-rules-runtime.sh
```

Esse smoke grava uma definicao e uma materializacao governadas. Por padrao,
o script tenta primeiro validar `POST /api/praxis/config/domain-rules/simulations`
e depois segue com o fluxo `definition -> materialization`. Ele tambem tenta
validar `POST /api/praxis/config/domain-rules/publications` com uma segunda
shared rule de `procurement.suppliers` em `selection_eligibility`, exigindo
que a publicacao materialize `option_source` para `supplier`. Por padrao, ele usa
`REQUIRE_SIMULATION=auto` e `REQUIRE_PUBLICATION=auto`: se o runtime ainda nao
expuser `/simulations` ou `/publications`, registra warning e continua no
baseline `definition -> materialization`; use `true` quando o host ja dever
suportar esses endpoints obrigatoriamente. O script tambem usa `SMOKE_RUN_ID`
com timestamp UTC para evitar colisao com execucoes anteriores. Para reproduzir
exatamente uma execucao, informe `SMOKE_RUN_ID`, `RULE_KEY`,
`MATERIALIZATION_KEY`, `PUBLICATION_RULE_KEY` ou `PUBLICATION_OPTION_SOURCE_KEY`.
Quando a simulacao estiver disponivel, o smoke exige tambem o bloco
`explainability`, incluindo `summary`, `recommendedAction`,
`publicationReadiness` e ao menos um item em `nextSteps`. No caminho de
publicacao de procurement, ele tambem exige `targetLayer=option_source`,
`targetArtifactType=resource-option-source`, `targetArtifactKey=supplier`,
`status=applied` e `materializedPayload.kind=lookup_selection_policy`. Em seguida,
o smoke consulta `/api/procurement/suppliers/option-sources/supplier/options/filter`
com o tenant isolado da publicacao e exige que um fornecedor com status bloqueado
pela decisao publicada retorne `extra.selectable=false`, provando que a policy
materializada governa o lookup em runtime.

O quickstart tambem possui consumo runtime para `backend_validation`: quando
existir materializacao aplicada com `targetLayer=backend_validation`,
`targetArtifactType=resource-validation`,
`targetArtifactKey=procurement.purchase-orders` e
`materializedPayload.kind=resource_validation_policy`, o comando de pedido de
compra valida o `supplierId` contra a policy publicada e rejeita fornecedores
em status bloqueado com `409 Conflict`. Esse ponto e intencionalmente
downstream: a definicao, simulacao, aprovacao, publicacao e derivacao da
materializacao continuam pertencendo ao `praxis-config-starter`. A comprovacao
end-to-end desse caminho fica no gate `REQUIRE_BACKEND_VALIDATION=auto|true|false`.
Em `auto`, o smoke cria/publica uma regra de validacao para
`procurement.purchase-orders`, consulta a materializacao aplicada e so tenta o
comando mutavel de pedido se a publication ja tiver derivado
`resource_validation_policy`; se a versao publicada do starter ainda nao fizer
essa derivacao, o script registra warning e evita escrever pedido de teste no
managed PostgreSQL. No corte corrente do host (`praxis-config-starter:0.1.0-rc.71`), a derivacao automatica de
`backend_validation` esta publicada com `source_hash` derivado por digest
estavel. Quando o gate chega ao comando mutavel de pedido, o smoke autentica
em `/auth/login` com `ADMIN_PASSWORD` ou `PRACTICE_TEMP_PASSWORD`, preservando a
politica de seguranca do host para `POST /api/procurement/purchase-orders`.
Com essa credencial disponivel, o gate recomendado passa a ser `true`.

O mesmo smoke tambem cobre actions operacionais governadas: com
`REQUIRE_WORKFLOW_ACTION=auto|true|false`, ele cria/publica uma regra
`workflow_action_policy` para `human-resources.folhas-pagamento:mark-paid`,
exige materializacao aplicada em `targetLayer=workflow_action`,
`targetArtifactType=resource-workflow-action`,
`targetArtifactKey=human-resources.folhas-pagamento:mark-paid`,
`materializedPayload.kind=workflow_action_policy` e `sourceHash` derivado. Quando
o gate consegue autenticar, ele chama
`POST /api/human-resources/folhas-pagamento/{id}/actions/mark-paid` no tenant
isolado da publicacao e espera `409 Conflict`, provando que o host consome a
decisao materializada sem se tornar fonte primaria da regra.

O workflow manual `Domain Rules Runtime Smoke` expõe os mesmos gates via
inputs `require_simulation`, `require_publication` e
`require_backend_validation`, alem de `require_workflow_action` para o caminho de
actions governadas. No host alinhado a `praxis-config-starter:0.1.0-rc.71`, os
gates de simulation, publication e backend validation recomendados sao `true`;
quando a derivacao `workflow_action` tambem estiver publicada no host, o gate
recomendado passa a ser `require_workflow_action=true`.

Verificacao runtime da camada federada:

```bash
BACKEND_URL=http://localhost:8088 scripts/verify-domain-federation-runtime.sh
```

Esse smoke grava uma release federada candidata, consulta auditoria, consulta o
relatorio de validacao persistido e ativa a release. O runtime precisa estar
com `praxis.domain-federation.persistence.enabled=true`. Por padrao, o script
usa `SMOKE_RUN_ID` com timestamp UTC e deriva o tenant de publicacao a partir
desse identificador para evitar colisao entre execucoes
com execucoes anteriores.

## Resultado validado em 2026-04-22

- `human-resources.funcionarios`: 426 itens, 16 governance.
- `human-resources.folhas-pagamento`: 175 itens, 3 governance.
- `operations.missoes`: 403 itens, 11 governance.
- `operations.acordos-regulatorios`: 148 itens, 4 governance.
- `procurement.suppliers`: 117 itens, 5 governance.

As verificações retornaram governança para privacidade/LGPD/GDPR, dados financeiros, compliance regulatório, política interna e uso permitido/restrito por IA.

Validação adicional com `praxis-config-starter:0.1.0-rc.19`, em modo sem vector store:
- `human-resources.funcionarios` ingerido em `2:47.65`;
- `426` itens persistidos;
- governança de `cpf` retornando LGPD/GDPR, `visibility=mask`, `trainingUse=deny` e `ruleAuthoring=review_required`;
- projeção `domain_knowledge_*` validada com `concepts=60`, `aliases=103`, `bindings=59`, `relationships=59` e `evidence=119`;
- logs de boot sem inicialização/validação de `PgVectorStore`.

## Resultado validado em 2026-04-23

Camada de regras compartilhaveis validada com `praxis-config-starter:0.1.0-rc.22`
e `praxis-api-quickstart` apontando para o runtime publicado em Render.

- Flyway remoto validado ate `V20__create_domain_shared_rule_layer.sql`.
- Banco de configuração contem `domain_rule_definition` e
  `domain_rule_materialization`.
- Smoke local contra Render concluiu com `shared-rule-runtime-ready`.
- Workflow manual `Domain Rules Runtime Smoke` concluiu verde no run
  `24819119653`.
- Fluxo validado: criar definição, ativar definição, criar materialização
  `form_config` e aplicar materialização por `PATCH`.
- Correção de runtime multi-datasource validada: `DomainRuleService` usa
  explicitamente `configTransactionManager`, evitando `LazyInitializationException`
  ao montar resposta de materialização aplicada.

Camada de federacao de dominio validada com `praxis-config-starter` local e
`praxis-api-quickstart` empacotado como host operacional.

- Flyway remoto validado ate `V21__create_domain_federation_read_model.sql`.
- Banco de configuracao contem `domain_federation_release`, `domain_source`,
  `domain_context`, `domain_context_relationship`, `domain_contract` e
  `domain_resolution`.
- Fluxo validado: bloquear origem nao permitida, validar dry-run, persistir
  candidate, consultar auditoria, consultar validacao persistida e ativar
  release.
- Contagem persistida do smoke: `sources=4`, `contexts=4`,
  `relationships=2`, `contracts=2` e `resolutions=2`.

## Persistência

O banco de configuração precisa conter:
- Flyway `V17__create_domain_catalog.sql` aplicado com sucesso;
- Flyway `V18__create_domain_knowledge_projection.sql` aplicado com sucesso para materializar `domain_knowledge_*`;
- Flyway `V20__create_domain_shared_rule_layer.sql` aplicado com sucesso para regras compartilhaveis;
- Flyway `V21__create_domain_federation_read_model.sql` aplicado com sucesso para o read model federado;
- tabela `domain_catalog_release`;
- tabela `domain_catalog_item`.
- tabelas read-only `domain_knowledge_*` quando `praxis.domain-knowledge.projection.enabled=true`.
- tabelas `domain_rule_definition` e `domain_rule_materialization` quando o runtime expuser `/api/praxis/config/domain-rules/**`.
- tabelas `domain_federation_release`, `domain_source`, `domain_context`,
  `domain_context_relationship`, `domain_contract` e `domain_resolution`
  quando o runtime expuser `/api/praxis/config/domain-federation/**`.

O banco remoto pode estar à frente do Flyway local. Antes de executar migrations, validar o histórico remoto. Para os gates descritos aqui, use `SPRING_FLYWAY_ENABLED=false`.

## Limites explícitos

Este contrato ainda não representa:
- executor de regras compartilhaveis;
- autorização RBAC/ABAC;
- decisão OPA/Rego;
- aprovação humana de regra criada por LLM;
- UI operacional para revisar, promover ou reprovar regra criada por LLM;
- execucao de regras federadas entre servicos;
- reconciliacao automatica entre servicos sem revisao semantica.

Essas camadas devem nascer sobre o catálogo semântico, não misturadas dentro dele.

No posicionamento arquitetural atual, `domain-rules` deve evoluir como a
fronteira canônica de decisão compartilhada em `/api/praxis/config/**`,
incluindo simulação, definição governada e materialização derivada. O
authoring de componente em `/api/praxis/config/ai/authoring/**` continua
relevante para página/componente, mas não deve voltar a ser o trilho primário
de regra de negócio.

## Checklist de fechamento de release

- Metadata starter publicado no Maven Central.
- Config starter publicado no Maven Central.
- Quickstart consumindo as versões publicadas.
- Exemplo AI-ready documentado em [`AI-READY-MISSION-DOMAIN-CATALOG.md`](AI-READY-MISSION-DOMAIN-CATALOG.md).
- Guia de autoria para analistas e LLMs documentado em [`LLM-DOMAIN-AUTHORING-GUIDE.md`](LLM-DOMAIN-AUTHORING-GUIDE.md).
- `OpenApiGroupResolutionIsolatedIntegrationTest` verde.
- `AiPatchSchemaResolutionIsolatedIntegrationTest` verde.
- `RagVectorStoreDisabledWiringIntegrationTest` verde.
- `IntegrationTestIsolationPolicyTest` verde.
- `scripts/ensure-domain-catalog-context.sh` executado contra runtime com Flyway desligado.
- `scripts/verify-domain-catalog-context.sh` verde contra catálogo persistido.
- `scripts/verify-domain-catalog-authoring-runtime.sh` verde contra runtime publicado.
- `scripts/verify-domain-rules-runtime.sh` verde contra runtime com `V20` aplicado.
- `scripts/verify-domain-federation-runtime.sh` verde contra runtime com `V21`
  aplicado e persistencia de federacao habilitada.
- `scripts/verify-domain-knowledge-change-set-runtime.sh` verde contra runtime
  com `domain_knowledge_*` projetado e endpoints de change set publicados pelo
  `praxis-config-starter`.
- workflow `Domain Catalog Runtime Smoke` verde contra o host operacional.
- workflow manual `Domain Rules Runtime Smoke` verde contra o host operacional
  depois do deploy da versao que contem `/api/praxis/config/domain-rules/**`.
- CI remoto verde na `main`.
- README e este documento atualizados.
