# Policy Studio — caso de referência do Rule Lab

O Rule Lab de auxílio extraordinário é o caso neutro usado para evoluir o
Praxis Policy Studio sem depender do ambiente Oracle do ErgonX.

A estrutura operacional permanece em `ExtraordinaryGrantRuleSetFactory`, mas as
sete condições JSON Logic editáveis são materializadas pelo
`ExtraordinaryGrantRuleSetComposer` a partir das definições aprovadas do Config.
Bindings Java e bindings fixos de composição continuam host-owned e passam pelos
registries de planejamento e execução. O runtime de snapshots e as suítes
`rule-lab-*` fecham essa fronteira. O arquivo
`src/test/resources/policy-studio/extraordinary-benefit-policy-studio-projection.v1.json`
é somente uma projeção derivada para inspeção: não é contrato público, não é
fonte de execução e não autoriza publicação ou ativação.

`PolicyStudioProjectionContractTest` impede drift de identidade, slots,
bindings, ordem, facts e hashes das evidências. O Studio sincroniza esse arquivo
com `npm run sync:quickstart-projection`.

Remover essa ponte quando Config e Metadata publicarem discovery governado
equivalente. Até lá, qualquer mudança na factory ou nas matrizes deve atualizar
a projeção no mesmo corte e passar o teste focal.

No perfil `dev`, `PolicyStudioRuleLabDefinitionSeed` publica idempotentemente os
sete bindings JSON Logic editáveis pelo `DomainRuleService`, no escopo
`desenv/local`. Não há SQL direto, promoção de status ou mudança de autoridade.
O seed pode ser desativado por `PRAXIS_RULE_LAB_POLICY_STUDIO_SEED_ENABLED=false`.

## Change workspace e cenários governados

O Config Starter publica workspaces ancorados no fingerprint da definição e cenários reutilizáveis
com resultado esperado nos cinco estados do Rules Engine. O Quickstart protege as leituras com
`ROLE_RULE_DEFINITION_READER` e as mutações com `ROLE_RULE_DEFINITION_AUTHOR`, inclusive quando
`app.security.read-open=true`.

O sandbox host-owned consome esses cenários e compara o candidato do workspace com o snapshot
ativo usando os mesmos facts, relógio e timezone. O baseline legado é opcional e precisa ser
independente; o Quickstart não inventa um oracle copiando a decisão candidata.

## Sandbox candidate × active

`POST /api/praxis/policy-studio/sandbox/runs` é a fronteira read-only do host para executar os
cenários de um workspace. A operação exige `ROLE_RULE_DEFINITION_AUTHOR`, resolve o escopo pelo
principal do servidor e nunca executa efeitos ou persiste recursos operacionais.

O Quickstart substitui somente o binding cujo `ruleKey` existe no RuleSet neutro, compila o grafo
completo com o registry executável do host e captura uma única sessão do snapshot ativo para toda a
bateria. Facts, instante UTC e timezone são compartilhados entre candidato e ativo. O resultado
preserva `MATCH`, `MISMATCH`, `INCONCLUSIVE` e `TECHNICAL_ERROR`; indisponibilidade do snapshot
ativo jamais é convertida em `DENY`.

O retorno inclui a revisão e fingerprint do workspace, identidade do snapshot ativo, decisões,
outputs por binding, reason codes, intenções de efeito planejadas, plan digests e facts digest.
Intenções só são extraídas de outputs `PLANNED_NOT_EXECUTED`; nenhum effect executor ou ledger
operacional é acionado pelo sandbox. Facts e payloads de snapshot não são devolvidos. A mesma
operação registra um Test Run imutável no Config, vinculado à revisão e ao fingerprint exatos do
workspace. O Config recompõe os matches contra o cenário persistido e a submissão rejeita evidência
obsoleta ou qualquer divergência de decisão, output, reasons ou efeitos esperados do candidato.
O Quickstart identifica esse baseline como `SYNTHETIC_EXPECTED` e calcula um digest estável das
expectativas selecionadas. Ele nunca publica `LEGACY_ORACLE`. Evidência operacional de
`CREATE`/`UPDATE` pertence aos fluxos persistentes do host e pode ser anexada ao Test Run `V58`
somente depois de verificar before/after, mutação ou não mutação, cleanup e ledger de efeito.

O request exige uma `idempotencyKey` opaca e um `evaluatedAtUtc` congelado e estável para o mesmo
comando. Antes de reavaliar, o host procura um receipt já persistido sob a chave e o escopo; se
instante, timezone e conjunto de cenários coincidirem, devolve o run original. Em uma corrida sem
receipt ainda visível, o Config também devolve o original quando chave e hash coincidem e rejeita
com `409` quando a chave é reutilizada com outro payload. O Studio conserva chave e instante em
falhas de transporte e os rotaciona juntos após sucesso ou mudança de workspace/cenários.

## Pré-requisitos da prova local

O principal `admin` do Quickstart local possui leitura e autoria de definições, mas não aprovação,
publicação ou operação de snapshots. Essa separação é intencional: o autor consegue criar o
workspace, persistir o draft, manter cenários e executar o sandbox, mas não consegue aprovar ou
ativar sua própria mudança.

Para que a comparação candidate × active seja elegível à submissão, deve existir um snapshot ativo
do RuleSet `extraordinary-grant-eligibility`. Ele é provisionado pelo fluxo maker-checker oficial
`scripts/workspace/Initialize-RuleLabQl07Snapshot.ps1`, que exige duas identidades aprovadoras
distintas e uma identidade publicadora configuradas no ambiente. Sem esse baseline, o sandbox
registra `TECHNICAL_ERROR` para active e o Config bloqueia a submissão; não se deve contornar esse
gate ou converter a indisponibilidade em sucesso.

O provisionador não cita mais definições genéricas. Ele resolve exatamente os
sete `ruleKey` do Policy Studio, garante aprovação autenticada, entrega esses
DTOs ao compositor Java do host e publica o grafo resultante junto dos sete IDs
de origem. O compositor falha se faltar uma decisão, houver duplicidade, status
não publicável, escopo/host incompatível ou condição vazia. Assim, uma condição
aprovada no Studio é a condição que entra no snapshot; o PowerShell não remonta
JSON Logic nem edita o payload executável.

A primeira prova PostgreSQL/HTTP das asserções semânticas da V56 está registrada
em [`POLICY-STUDIO-V56-NEON-INTEGRATION-EVIDENCE.md`](POLICY-STUDIO-V56-NEON-INTEGRATION-EVIDENCE.md).
Ela confirma a migração, os matches de decisão/output/reasons/effects, ETag e
autorização fail-closed. A continuação provisionou o head maker-checker e provou
o workspace até `PROMOTED`; publication readiness permaneceu corretamente
bloqueada por cobertura aprovada existente.

## Revisão e promoção

O Studio consome as operações canônicas de review e promotion do Config. A
revisão persiste decisão, justificativa, identidade do revisor, revisão do
workspace e fingerprint da definição-base. O formulário pode ser apresentado a
partir do estado `SUBMITTED`, mas somente o servidor decide se o principal possui
`ROLE_RULE_DEFINITION_APPROVER` e se é diferente do autor.

Uma aprovação não publica nem ativa o RuleSet. A promoção cria uma nova versão
governada da definição; publicação, composição, duas aprovações de snapshot e
ativação permanecem etapas distintas do control plane.

## Observabilidade host-owned

O runtime registra avaliações e refreshes de snapshot pelo Micrometer. As métricas
`praxis.rule.runtime.evaluations`, `praxis.rule.runtime.evaluation.duration` e
`praxis.rule.runtime.snapshot.refreshes` usam somente dimensões de baixa cardinalidade:
identidade estável do RuleSet, outcome/status e resultado do refresh. Elas não carregam facts,
referência do pedido, ator, snapshot, hash ou payload executável.

O health indicator continua sendo a projeção local segura de readiness e last-known-good. O
`RuleHostStatusReporter` agora publica periodicamente esse estado redigido no contrato canônico do
Config, fora da avaliação. O Config compara snapshot, hash e revisão com o head ativo e também
contrato do host, engine, dialeto, corpus e catálogo de implementações com o manifesto aprovado.
O Policy Studio recebe somente contagens agregadas de hosts alinhados, com snapshot em drift,
runtime incompatível, indisponíveis ou vencidos.
O Studio não consulta Actuator de cada consumidor e não recebe hostnames, IPs ou identidades dos
atores internos.

## Publication readiness

Depois da promoção, o Studio chama a simulação estrutural canônica do Config e
exibe `existingCoverage`, `predictedMaterializations`, `requiredApprovals`,
`warnings`, ação recomendada e `publicationReadiness`. Esse resultado não recebe
facts e não deve ser descrito como prova de outcome; essa responsabilidade é do
sandbox host-owned e dos Test Runs.

Somente `ready_to_publish` habilita a solicitação de publicação no cliente. O
servidor reavalia readiness, identidade e papel publicador, podendo ainda devolver
uma publicação bloqueada. Uma publicação bem-sucedida processa a definição e as
materializações elegíveis, mas não cria nem ativa automaticamente o snapshot do
RuleSet.
## Evidência operacional de execução

O caso de referência agora separa explicitamente avaliação e observabilidade governada. Cada
avaliação do snapshot ativo tenta registrar no banco operacional uma observação redigida contendo
somente `observationId`, escopo server-owned, identidade/hash/revisão do snapshot, outcome, duração
e instante. Facts, identificadores de negócio, reason codes e payload executável não entram no
outbox.

A indisponibilidade do banco operacional ou do Config nunca muda o outcome nem derruba a avaliação.
Um worker governado chama `RuleExecutionObservationDispatcher.dispatchNext()`; ele usa lease,
idempotência no Config e retry limitado para entregar a evidência ao control plane. Como no outbox
de statements, o Quickstart não habilita um scheduler implícito: cada implantação deve executar o
dispatcher no seu job runtime supervisionado.

Essa prova materializa o caminho `host runtime -> outbox operacional -> Config execution summary`
sem transformar o Policy Studio, o navegador ou o Rules Engine em owner de telemetria persistida.
Execuções candidate/active do sandbox usam uma lane explícita sem observação operacional; seus
resultados permanecem governados pelo Test Run e não contaminam métricas de produção.

`RuleExecutionObservationPostgresIntegrationTest` fecha a prova com dois PostgreSQLs Testcontainers
independentes. O teste pausa fisicamente o banco do Config depois da avaliação, confirma que a
observação permanece `PENDING`, reativa o control plane, entrega com o mesmo `observationId` e exige
uma única linha no resumo canônico. O teste é ignorado automaticamente em máquinas sem Docker; um
gate de integração/release com Docker deve executá-lo sem skip.

Em paralelo, `RuleHostStatusReporter` publica heartbeat mesmo quando não há avaliação. A falha de
entrega é isolada e nunca altera a referência last-known-good ou o outcome. O intervalo e a
identidade interna do deployment são propriedades do host; tenant e environment são os mesmos do
loader e não vêm de uma requisição de negócio.

Na superfície HTTP, somente uma identidade de serviço com `RULE_EXECUTION_OBSERVER` pode publicar
o heartbeat. `RULE_SNAPSHOT_READER` continua sendo a autoridade separada para consultar o resumo
agregado. O admin demonstrativo não acumula a autoridade de observação, evitando que uma sessão
humana se passe por deployment. `SecurityConfigRuleSnapshotReadPolicyTest` prova anônimo negado,
leitor negado e observador autenticado aceito mesmo quando `read-open=true`; os testes de serviço
do Config provam escopo/ator server-owned e a classificação agregada. A persistência usa chave
única por tenant/environment/RuleSet/ator e `ON CONFLICT ... WHERE observed_at < EXCLUDED.observed_at`,
de modo que um heartbeat atrasado não substitui o mais novo. O gate
`RuleHostStatusPostgresHttpIntegrationTest` atravessa filtro HTTP, resolver de principal,
controller, transação e `ON CONFLICT` nativo. Ele cobre dois tenants, três identidades de host,
evento atrasado, heartbeat vencido, indisponibilidade e recuperação incompatível → alinhado.
Os atributos do request representam claims instalados por um adapter de autenticação confiável,
não headers aceitos do caller. Onde Docker não existe, o teste é reportado como skipped.

## Candidate preload isolado

`RuleCandidatePreloader` fecha a primeira implementação host-side do rollout em duas fases. Ele
recebe uma identidade de rollout/candidato coordenada pelo servidor, busca o snapshot imutável no
Config, confirma hash, tenant, environment, owner e RuleSet, compila com o registry executável e
publica o probe usando as coordenadas reais do runtime e o digest do catálogo admitido.

O preloader não recebe `ExtraordinaryGrantRuleSnapshotRuntime` e, portanto, não tem caminho para
trocar o last-known-good, avaliar facts ou executar efeitos. `RuleCandidatePreloaderTest` confirma
um preload válido e uma divergência de hash fail-closed, mantendo o status do runtime ativo
inalterado. A descoberta/supervisão de rollouts ainda precisa de um job operacional explícito;
nenhum scheduler oculto foi habilitado.

O Config agora publica `GET .../rollouts/pending?ruleSetKey=...` apenas para
`RULE_EXECUTION_OBSERVER`. A consulta usa o escopo do principal e omite rollouts expirados ou cuja
identidade ativa/ETag esperada já mudou. `RuleCandidatePreloadSupervisor.pollOnce()` transforma
essa projeção mínima no comando do preloader. Não há `@Scheduled`, loop ou executor interno: a
implantação deve chamar o passo por um job supervisionado e definir cadência, retry e shutdown.

`RuleStagedRolloutPostgresIntegrationTest` fecha a prova persistida com dois PostgreSQLs embedded:
publica snapshots imutáveis v1/v2, cria um rollout sob política `REQUIRED`, confirma que antes do
probe apenas cancelamento é permitido, executa o preloader real do Quickstart, redescobre readiness
e a ação server-owned de ativação, e promove o candidato usando rollout + ETag. O teste confirma no
banco o novo head, revisão de ativação 2, rollout terminal e exatamente um evento `ACTIVATED`.
Somente o recorte canônico das tabelas de snapshot/rollout é migrado nesse gate, pois as migrações
completas do host também exigem extensões operacionais, como pgvector, que não pertencem a esta prova.

## Gate transacional de ativação

O endpoint de ativação aceita `X-Rule-Rollout-ID`. Em `OBSERVE_ONLY`, chamadas antigas sem rollout
continuam válidas. Quando a política ativa do RuleSet for `REQUIRED`, o Config exige o rollout,
bloqueia head e rollout, confirma candidato/head/ETag/política/expiração, recalcula quorum e somente
então rotaciona o head. Probe concorrente precisa do mesmo lock e não pode alterar o quorum entre a
decisão e o commit. Evento de ativação e fechamento do rollout participam da mesma transação.
Rollback continua independente desse gate para preservar recuperação emergencial.

A política deixou de depender de alteração direta no banco. O Config expõe um lifecycle versionado
`DRAFT -> APPROVED -> ACTIVE -> SUPERSEDED`: autoria usa `RULE_DEFINITION_AUTHOR`, aprovação exige
outro ator com `RULE_DEFINITION_APPROVER`, e ativação usa `RULE_SNAPSHOT_OPERATOR` com o `If-Match`
forte de um head de política independente. A troca é bloqueada enquanto existir rollout aberto e o
limite máximo de idade da política passa a restringir a criação do rollout. O Policy Studio deve
consumir esse catálogo, ETag e timeline; ele não pode calcular quorum nem promover status no browser.
