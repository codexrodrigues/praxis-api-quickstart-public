# Rule Lab QL-08 — stress report Ergon-like

## Resultado

QL-08 conclui o nível L7 do laboratório neutro como `IMPLEMENTED_VALIDATED`, mas mantém a revisão
global P2F-13 e a Fase 9 em `BLOCKED`. O objetivo deste gate não é reproduzir nomes, tabelas ou
rotas legadas. Ele pressiona as mesmas classes estruturais de problema com fixtures fictícias e
classifica cada limite pelo owner canônico.

A matriz versionada está em
`src/test/resources/rule-lab/rule-lab-ql08-stress-matrix.json`. O teste
`RuleLabQl08StressMatrixTest` exige identidade estável, owner, finding, evidência JUnit executável e
decisão residual para toda capacidade ainda não comprovada. O gate também impede que o relatório
declare autoridade ou abra a Fase 9.

## Resumo executivo

| Classificação | Cenários | Interpretação |
| --- | ---: | --- |
| `VERIFIED_CURRENT_CONTRACT` | 10 | O contrato atual cobre o cenário neutro com teste executável. |
| `PARTIAL_REQUIRES_CONTRACT` | 0 | O P2F-ADR-11 já define o contrato canônico de transformação tipada. |
| `PARTIAL_REQUIRES_IMPLEMENTATION` | 0 | Nenhuma implementação neutra permanece aberta no laboratório. |
| `PARTIAL_REQUIRES_EXTERNAL_EVIDENCE` | 2 | Faltam replay legado e drills distribuídos na infraestrutura-alvo. |
| `BLOCKED_EXTERNAL_EVIDENCE` | 1 | A baseline sintética não pode substituir uma rota DB-backed real. |

O engine não recebeu I/O, persistência, transação, effect executor ou semântica específica do
Ergon. O Config Starter continua dono do snapshot/head; o Quickstart continua dono da prova de
host; facts, transações, efeitos e adapters de baseline continuam responsabilidade do host de
domínio.

## O que os contratos atuais suportam

- composição determinística de guards protegidos, produto e cliente;
- slots fechados e rejeição de bypass incompatível;
- separação entre `DENY`, `NOT_APPLICABLE`, `INCONCLUSIVE` e `TECHNICAL_ERROR`;
- cálculo puro e `EffectIntent` sem mutar facts nem executar efeito no engine;
- lote ordenado com transação independente por item e resultado parcial explícito;
- `STATEMENT_ATOMIC` com um snapshot/instante/timezone, barreira agregada e rollback integral;
- reserva/fingerprint/replay de `STATEMENT_ATOMIC`, com conflito para a mesma chave e comando diferente;
- commit atômico de itens, resultado idempotente e uma mensagem outbox mínima;
- lease recuperável, retry exponencial limitado, acknowledgement e dead-letter locais;
- adapter HTTP com TLS obrigatório por padrão, bearer token opcional, timeouts limitados, redirects
  desabilitados e classificação fechada de falhas permanentes/transitórias;
- consumidor laboratorial HTTP com inbox idempotente em datasource independente;
- reconciliação de resposta ambígua por `messageId`, sem I/O externo dentro da transação e sem roubar lease ativo;
- contexto de operação explícito, limitado e limpo após commit ou rollback, sem `ThreadLocal`;
- replay governado de dead-letter com quarentena, probe obrigatório, lock, actor/justificativa e
  auditoria append-only sem payload;
- ETag, idempotência e rollback local quando o ledger de efeito conflita;
- exatamente um efeito local quando dois `apply` concorrem com o mesmo ETag;
- rejeição cross-tenant sem desalojar o snapshot last-known-good;
- ativação/hot reload atômicos e rollback para conteúdo imutável anterior;
- shadow com timeout, redaction e nenhuma mutação operacional.

## Lacunas canônicas ainda abertas

### 1. Transformação tipada antes da escrita

O P2F-ADR-11 foi publicado em `0.1.0-beta.12`. O Quickstart consome essa
coordenada pública e substituiu o output genérico por
`grant.amount-transformation`, executada somente depois de orçamento aprovado.
O engine valida snapshot e imutabilidade; o adapter do host allowlista proposta,
binding, campo, schema, operação, ausência anterior e limites monetários. A
transação existente persiste o valor materializado e uma evidência append-only
redigida na mesma unidade. A auditoria preserva identidade da proposta, snapshot,
digests de facts/plano e os digests canônicos `before`/`after` do engine, sem request
reference, valor ou facts serializados. Replay não duplica a evidência e rollback
agregado a remove junto com o write de negócio. Um host continua proibido de interpretar livremente
`JsonNode` como mutação de linha.

### 2. Efeito externo e auditoria autônoma

A outbox, o dispatcher at-least-once, o adapter HTTP e a reconciliação estão implementados no host.
O laboratório executa HTTP real contra um consumidor fictício com datasource independente: ele
deduplica por `messageId`, persiste o acknowledgement antes da resposta e permite reconciliar o
caso em que o commit externo ocorreu, mas a resposta foi perdida. Isso prova o contrato neutro de
`EXTERNAL_DELIVERED`; ainda não comprova efeito idempotente no ERP, folha ou banco externo reais,
nem retenção, restart, rotação de credenciais e runbook na infraestrutura corporativa-alvo.

### 3. Baseline DB-backed real

A baseline sintética prova a mecânica segura do dual-run, não paridade. A primeira regra Ergon
continua exigindo adapter real sanitizado, fixture/restore, rota de leitura/escrita conhecida e
cleanup comprovado. Sem isso, preflight, authority e desligamento legado permanecem proibidos.

## Evolução executável do P2F-ADR-10

Após o stress report inicial, o host adicionou uma prova interna — sem endpoint — que mantém o lote
público `atomic=false` e introduz uma unidade separada `STATEMENT_ATOMIC`. Todos os itens usam o
mesmo snapshot capturado por referência imutável antes do primeiro item, instante e timezone,
juntam a mesma transação e atravessam uma barreira agregada
após `LOCAL_FLUSHED`. Falha nessa barreira reverte todos os itens.

A sessão capturada permanece no plano `v1` mesmo se o hot reload ativar `v2` durante a operação.
Assim, a evidência agregada não serve apenas para detectar mistura depois do fato: a avaliação de
cada item fica estruturalmente presa ao mesmo `CompiledRuleSnapshot`.

O contexto imutável é passado explicitamente e o registry de escopos é limitado a 64 operações.
Ele não usa `ThreadLocal`, não armazena facts/payload de effect e remove o escopo em `close`, tanto
no commit quanto no rollback. Isso comprova cleanup do fluxo normal; reconciliação após crash e
estado distribuído continuam fora deste slice.

O serviço interno reserva a execução antes da transação de domínio. A `executionId` torna-se a
`operationId`; itens, mensagem outbox e payload de replay são confirmados juntos. A mesma chave e
fingerprint restauram o resultado sem nova escrita; fingerprint diferente produz conflito. Em
falha, a transação de negócio e a outbox revertem e o ledger registra `FAILED` separadamente.

A mensagem carrega somente IDs, escopo, correlação e evidência do snapshot. Facts, referência de
negócio e token de lease não atravessam o adapter. O dispatcher faz claim em transação curta,
entrega fora da transação e confirma ou reagenda por lease em nova transação.

Os limites operacionais são explícitos por `PRAXIS_RULE_LAB_OUTBOX_MAXIMUM_ATTEMPTS`,
`PRAXIS_RULE_LAB_OUTBOX_LEASE_MS`, `PRAXIS_RULE_LAB_OUTBOX_RETRY_BASE_MS` e
`PRAXIS_RULE_LAB_OUTBOX_RECONCILIATION_SCAN_LIMIT`, todos validados no bootstrap. O adapter HTTP é
opt-in por `PRAXIS_RULE_LAB_OUTBOX_HTTP_ENABLED=true` e exige URL HTTPS, salvo habilitação insegura
explícita exclusiva para laboratório. URL, bearer token e timeouts vêm de configuração externa.
Respostas de autenticação/contrato e acknowledgements inválidos seguem direto para dead-letter;
throttling, timeout, transporte e indisponibilidade preservam somente o retry limitado.
O proof não ativa scheduler silenciosamente: o deploy precisa fornecer jobs governados para chamar
dispatcher e reconciler, com execução singleton ou coordenação equivalente.

## Provas combinadas executadas

O teste focal cobre cinco combinações que eram insuficientemente explícitas antes de QL-08:

1. dois `apply` concorrentes partem do mesmo ETag; somente um confirma efeito e transição;
2. conflito de effect ledger reverte status, versão e transição, preserva o ETag aprovado e marca a
   execução idempotente como `FAILED`;
3. read-after-failure confirma que o agregado continua `APPROVED`, sem falsa visibilidade de
   `APPLIED`;
4. `STATEMENT_ATOMIC` confirma dois itens somente depois da barreira agregada;
5. falha na barreira agregada reverte os dois itens e limpa o contexto explícito;
6. hot reload para `v2` não altera uma operação que já capturou a sessão `v1`.
7. replay de statement restaura a mesma `operationId` e `messageId`, sem duplicar itens/outbox;
8. chave reutilizada com fingerprint diferente falha em conflito;
9. falha agregada reverte itens/outbox e preserva somente o ledger técnico `FAILED`;
10. retries preservam `messageId`, encerram em dead-letter e lease expirado é recuperado;
11. acknowledgement marca `DELIVERED` e impede nova entrega local.
12. o consumidor HTTP persiste em inbox independente antes de simular perda da resposta;
13. a reconciliação consulta esse inbox, marca `DELIVERED` e impede redelivery do efeito já aceito.

O outbox e o inbox usam datasources H2/PostgreSQL-compatible distintos e comunicam-se por HTTP
real. Eles não fazem claim de atomicidade distribuída: a segurança deriva de at-least-once,
deduplicação durável e reconciliação.

## Validação reproduzível

Com JDK 21:

```powershell
mvn "-Dtest=RuleLabQl08StressMatrixTest,RuleLabGoldenContractTest,ExtraordinaryGrantRuleLabServiceTest,ExtraordinaryGrantRuleSnapshotRuntimeTest,ExtraordinaryBenefitShadowComparisonServiceTest,ExtraordinaryBenefitRequestPilotIntegrationTest" test
mvn -DskipTests package
```

Resultado atualizado em 2026-07-15:

- suíte QL-08 completa: 58 testes, zero falha, zero erro e zero skip;
- estrutura PostgreSQL da migration `V20260715_001` exercitada numa branch Neon schema-only
  efêmera: seis statements concluídos; `UPDATE` e `DELETE` rejeitados pelo trigger; uma linha
  original preservada e três digests válidos; a branch e a fixture fictícia foram excluídas após a
  coleta. O schema final sem duplicação do ator foi recompilado e revalidado pela integração focal;
- P2F-ADR-12 formalizado e migration `V20260715_002` validada numa branch Neon schema-only
  efêmera (`br-lively-cake-adzzma3y`) em matriz single-session: imutabilidade direta, expurgo limitado dos dois ledgers,
  legal hold, rejeição de hold órfão, rollback de execução duplicada e ledger append-only passaram;
  os dez invariantes finais ficaram verdadeiros, inclusive separação entre runtime insert-only,
  auditoria read-only, compliance e retenção. A branch e as fixtures fictícias foram excluídas após
  a coleta;
- corrida P2F-ADR-12 validada em 2026-07-15 com duas conexões PostgreSQL reais
  numa branch Neon `schema-only` efêmera: `PLACE` vencedor protegeu a linha via
  `SKIP LOCKED` e permitiu expurgo somente após `RELEASE`; expurgo vencedor fez
  `PLACE` aguardar o commit e falhar sem hold ou evento órfão. O runner produziu
  evidência sanitizada, terminou com `ADR12_CONCURRENCY_PROOF_PASS` e a branch
  foi excluída após a coleta;
- matriz de papéis P2F-ADR-12 validada em 2026-07-15 numa branch Neon
  `schema-only`: quatro capability roles `NOLOGIN` provaram append, leitura,
  legal hold e retenção nos respectivos limites; dez tentativas cruzadas foram
  rejeitadas com SQLState `42501`, nenhum papel possuía objetos e o cleanup foi
  concluído. O template continua parametrizado para não inventar nomes do
  ambiente corporativo;
- rotação HMAC P2F-ADR-12 implementada no contrato operacional: canonicalização
  `V1` com separação de domínio e propósito, segredo mínimo de 256 bits, key id
  opaco persistido, backfill explícito como `LEGACY-UNVERSIONED` e remoção das
  assinaturas SQL sem provenance. O harness prova o upgrade com três registros
  preexistentes, inclusive nos ledgers append-only. Testes focais
  usam somente segredos e referências fictícios; o secret manager real continua
  fora do laboratório;
- pacote Spring Boot: `BUILD SUCCESS` com testes omitidos apenas porque a suíte
  focal já havia sido executada imediatamente antes;
- nenhuma dependência local ou versão Maven foi alterada;
- nenhuma superfície HTTP pública foi criada ou modificada.

## Decisão P2F-13

QL-08 fecha o laboratório neutro, mas não fecha a fundação completa. P2F-ADR-10 foi aceito em
2026-07-14 no owner canônico
`praxis-rules-engine/docs/p2f-adr-10-transactions-batches-and-effects.md`, sem promover capacidade
não implementada. Permanecem blockers:

- P2F-ADR-05 para protected extensions;
- validação do adapter no sistema externo real e política corporativa de retenção; restart, rotação e
  runbook laboratorial do P2F-ADR-10 foram concluídos em Neon. O host já oferece métricas bounded,
  snapshot de backlog sem payload e retenção limitada apenas de `DELIVERED`; scheduler, dashboards,
  alertas e console/job autorizado pertencem à operação-alvo; a SPI de replay auditado já existe no host;
- operação corporativa do P2F-ADR-12: workload identities e grants implantados
  no ambiente-alvo, secret manager/rotação HMAC reais, scheduler, SIEM, backup, alertas e
  aprovação da política; o contrato neutro, a fronteira SQL, a matriz
  laboratorial de papéis e a prova concorrente já estão implementados no host;
- P2F-09/RF-01–RF-05 para factory e provenance;
- baseline DB-backed Ergon e dispatcher corporativo real;
- owner do primeiro deployable/regra piloto;
- readiness report assinado e `part2-foundation-readiness-v1.json=READY`.

O drill distribuído recomendado foi concluído em 2026-07-15 com consumidor em processo separado,
HTTPS, dois PostgreSQL Neon isolados por branches efêmeras, restart, timeout pós-commit,
reconciliação e rotação de credencial. Isso fecha a prova operacional laboratorial do P2F-ADR-10,
mas não promove integração Ergon nem autoridade.

O harness e o runbook desse passo estão em
`scripts/workspace/Invoke-RuleLabQl08DistributedOutboxDrill.ps1` e
`docs/RULE-LAB-QL-08-DISTRIBUTED-OUTBOX-RUNBOOK.md`. Eles lançam uma JVM consumidora separada,
duas branches PostgreSQL Neon schema-only, TLS temporário e rotação de bearer token. O script rejeita
hosts fora do Neon ou iguais aos endpoints canônicos, não imprime credenciais e produz JSON redigido.
A execução comprovou `RETRY_SCHEDULED -> RECONCILED`, rejeição da credencial antiga com HTTP 401 e
entrega com a credencial nova; as duas branches foram excluídas após a coleta da evidência.
