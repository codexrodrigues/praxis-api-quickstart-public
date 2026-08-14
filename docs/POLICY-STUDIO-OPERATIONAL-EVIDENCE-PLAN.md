# Policy Studio — prova operacional CREATE/UPDATE

- Estado: aprovado para implementação incremental
- Data: 2026-08-14
- Classe: arquitetural e transversal

## Decisão

O sandbox do Policy Studio continua estritamente sem efeitos. A prova de
persistência pertence ao host e será executada por um adapter operacional do
Quickstart sobre o datasource PostgreSQL de domínio. O resultado sanitizado será
registrado no `DomainRuleTestRun` V57, cujo owner canônico é o Praxis Config.

Não será criado banco, schema, DTO de Test Run ou endpoint paralelo no Studio.
O Config e o datasource operacional podem residir no mesmo projeto Neon, mas
devem manter credenciais, ownership e migrations separados.

## Inventário de aderência

| Necessidade | Classificação | Situação |
| --- | --- | --- |
| Candidate versus active sem efeitos | `ja-suportado-so-ux` | sandbox host-owned e Test Run já implementados |
| Proveniência do baseline | `ja-suportado-so-ux` | Config V57 suporta sintético, snapshot ativo e oracle legado |
| Evidência sanitizada CREATE/UPDATE | `suportado-parcialmente` | DTO V57 existe; falta o produtor host-owned |
| Persistência CREATE governada | `ja-suportado-mal-nomeado-ou-mal-materializado` | `evaluate` avalia e persiste somente `ALLOW`, mas ainda não alimenta o Test Run operacional |
| Persistência UPDATE governada | `lacuna-real-de-contrato` | o domínio não possui comando de reavaliação concorrente; transições de lifecycle não equivalem a UPDATE |
| Cleanup de dados de teste | `suportado-parcialmente` | testes limpam tabelas, mas ainda não há identidade/protocolo de execução governado |

## Semântica obrigatória de UPDATE

O Quickstart deve ganhar uma action de item `re-evaluate`, disponível somente no
estado `EVALUATED`. Ela recebe o mesmo comando autoritativo usado no CREATE e:

1. exige `If-Match` e chave idempotente;
2. preserva `id` e `requestReference`;
3. readquire facts pelo provider host-owned e reavalia o snapshot vigente;
4. atualiza a linha somente quando o resultado for `ALLOW`;
5. mantém a linha inalterada para `DENY`, `NOT_APPLICABLE`, `INCONCLUSIVE` ou erro;
6. incrementa a versão JPA e registra a transformação append-only;
7. nunca executa o effect intent durante a prova;
8. rejeita registros `SUBMITTED`, `APPROVED` ou `APPLIED`.

`submit`, `approve` e `apply` não podem ser classificados como UPDATE de policy:
eles alteram lifecycle e, no último caso, materializam efeito local.

## Adapter operacional host-owned

O adapter recebe identidade canônica de workspace, scenario, modo de operação e
o principal governado. Ele não recebe SQL, nome de tabela, URL ou credencial do
browser. Para cada cenário, o host executa:

```text
capturar before digest
  -> executar CREATE ou re-evaluate UPDATE
  -> readback na mesma identidade de negócio
  -> capturar after digest e ledger digest
  -> verificar mutação ou não mutação esperada
  -> limpar apenas os registros criados pela execução
  -> verificar cleanup
  -> registrar Test Run V57 no Config
```

Os digests são SHA-256 de projeções canônicas e redigidas. Facts, valores
monetários, referências pessoais, payload HTTP, SQL e conteúdo de linha não
atravessam a fronteira para o Config.

## Mapeamento V57

| Campo | Fonte |
| --- | --- |
| `operationMode` | comando governado `CREATE` ou `UPDATE` |
| `beforeStateDigest` | readback redigido anterior; sentinel hash para ausência |
| `afterStateDigest` | readback redigido posterior |
| `mutationObserved` | comparação de digests e versão |
| `noMutationVerified` | readback idêntico após outcome não autorizador |
| `cleanupVerified` | ausência da linha e dos ledgers pertencentes à execução |
| `effectLedgerDigest` | projeção redigida de transformação/outbox/effect ledger |
| `baselineCallCount` | contador do adapter de baseline; zero não pode ser inferido |

O baseline sintético continua útil ao desenvolvimento local, mas não é prova de
paridade. Um adapter Ergon usará `LEGACY_ORACLE`, referência/digest do artefato
aprovado da Parte 1 e contagem real de chamadas ao legado.

## Matriz mínima executável

| Caso | Operação | Resultado | Mutação |
| --- | --- | --- | --- |
| elegível novo | CREATE | `ALLOW` | uma linha criada, efeito não executado |
| inelegível novo | CREATE | `DENY` | nenhuma linha criada |
| referência duplicada | CREATE | conflito idempotente | nenhuma segunda linha |
| elegível alterado | UPDATE | `ALLOW` | mesma identidade, versão e digest alterados |
| alteração inelegível | UPDATE | `DENY` | linha e versão inalteradas |
| `If-Match` obsoleto | UPDATE | `412` | linha inalterada |
| registro inexistente | UPDATE | `404` | nenhuma mutação |
| registro fora de `EVALUATED` | UPDATE | `409` | nenhuma mutação |
| replay da chave idempotente | CREATE/UPDATE | resposta anterior | nenhuma mutação adicional |
| cleanup | CREATE/UPDATE | verificado | linha, auditoria e execuções da prova removidas |

Todos os casos devem verificar tenant/environment, principal sem capability,
redaction, optimistic locking e que o sandbox continua com zero mutações.

## Banco e migrations

- migrations de domínio permanecem em `db/operational-migrations`;
- migrations do Config continuam no starter e são aplicadas pela identidade
  proprietária do schema;
- o runtime não recebe DDL nem ownership;
- a prova local usa PostgreSQL real descartável; a prova compartilhada usa os
  datasources Neon já configurados, sem criar banco por execução;
- cleanup usa uma `runId`/correlation id conhecida e nunca `TRUNCATE` ou filtros
  amplos em ambiente compartilhado.

## Segurança e capabilities

A futura action pública só poderá aparecer quando o backend publicar capability
específica, blockers e ETag. `ROLE_ADMIN`, origem permitida ou sessão válida não
substituem autorização por operação. O adapter do Ergon deve seguir a mesma
fronteira e nunca receber credenciais Oracle do Studio.

## Sequência de implementação

1. `re-evaluate` no aggregate Quickstart com ETag, idempotência e prova HTTP;
2. implementar o adapter operacional interno e o mapeamento V57;
3. provar a matriz CREATE/UPDATE em PostgreSQL descartável;
4. executar a mesma prova no Neon com registros identificados pela execução;
5. somente então publicar capability/action para o Studio;
6. entregar ao agente Ergon o SPI, corpus e formato de evidência, mantendo o
   adapter Oracle no host Ergon.

## Gate de pronto

O corte só está pronto quando um `ALLOW` e um `DENY` forem provados em CREATE e
UPDATE, o Test Run persistido carregar evidência V57 verdadeira, o cleanup for
confirmado, um principal sem capability receber `403`, um ETag obsoleto receber
`412` e nenhuma informação sensível aparecer no Config ou nos logs.

### Estado do primeiro incremento

O comando `re-evaluate` está implementado como action item-level: replay precede
a validação do ETag, a identidade é preservada, facts são readquiridos e somente
`ALLOW` altera a linha. A migration V20260814_001 permite múltiplas evidências
append-only para fatos distintos sem enfraquecer o ledger idempotente do comando.
O adapter que transforma essa execução em `DomainRuleOperationalTestEvidence`
V57 e a prova compartilhada no Neon permanecem no incremento seguinte.

## Adapter host-owned V57

O Quickstart possui agora uma fronteira interna para converter uma execucao operacional em
`DomainRuleOperationalTestEvidence`. Ela aceita somente digests SHA-256 sanitizados, modo
`CREATE` ou `UPDATE`, contagem de chamadas ao baseline e callbacks de comando/probe/cleanup.
Payloads de negocio, SQL, credenciais e identificadores pessoais nao atravessam essa fronteira.
Se a mutacao observada divergir da expectativa governada do cenario, o adapter falha fechado e
nao produz uma evidencia registravel.

O cleanup roda inclusive quando o comando falha e somente e considerado comprovado quando o
estado final coincide com o estado limpo esperado, capturado antes da preparacao descartavel. Isso
permite que uma prova de `UPDATE` prepare um recurso e ainda exija sua remocao ao final, em vez de
confundir o estado preparado com o estado que deve sobreviver. Esta primeira etapa ainda nao afirma
prova PostgreSQL ou Neon: o proximo corte conectara o probe ao recurso de beneficio extraordinario
com uma correlacao exclusiva da execucao e registrara a evidencia no Test Run governado.

O probe do beneficio extraordinario ja materializa a parte de observacao e cleanup: ele aceita
somente referencias reservadas com prefixo `policy-studio-proof-`, projeta recurso, auditoria de
transformacao e ledger de efeitos em digests e remove apenas linhas relacionadas a essa referencia.
O teste local H2 em modo PostgreSQL comprova isolamento, mudanca do digest e retorno ao estado
limpo; ele nao substitui a proxima prova em PostgreSQL real/Neon nem o registro no Config.

O orquestrador interno reutiliza `ExtraordinaryBenefitWorkflowService` para executar tanto a
criacao quanto a reavaliacao do mesmo agregado. Para `UPDATE`, um seed obrigatoriamente `ALLOW` e
persistido pelo proprio workflow antes da reavaliacao; se o seed nao produzir recurso, a prova
falha fechada e limpa qualquer vestigio correlacionado. O teste de integracao do piloto comprova
CREATE/UPDATE, facts autoritativos, snapshot ativo, mutacao observada e tabelas vazias ao final.

Um gate adicional executa o probe contra um processo PostgreSQL real e descartavel. Ele comprova
a sintaxe nativa das consultas, a transacao de cleanup e que uma referencia de negocio nao
correlacionada permanece intacta. Essa prova local real-processo nao usa nem altera o Neon
compartilhado; a lane Neon continua sendo o gate posterior de ambiente gerenciado.

O recorder interno recebe o resultado tecnico ja avaliado, exige cobertura operacional exata para
todos os cenarios e delega a gravacao ao `DomainRuleTestRunService`. Ele nao persiste uma segunda
entidade no host, nao aceita cobertura parcial e nao reimplementa as validacoes V57 do Config.

O executor composto mantem o sandbox comum read-only e recebe bindings operacionais host-owned
explicitos. Nenhum modo e inferido de labels, chaves ou facts. A avaliacao candidate/active ocorre
primeiro; somente uma cobertura de bindings exatamente igual ao conjunto avaliado autoriza os
comandos descartaveis e o registro V57.

`PolicyStudioSandboxService.prepare` e a unica preparacao candidate/active. O `run` read-only a
persiste diretamente como antes; o executor operacional reutiliza o mesmo request ainda nao
persistido, anexa as evidencias e grava uma unica vez. Nao existe Test Run tecnico intermediario.

O round-trip V57 tambem e coberto em PostgreSQL real-processo com os repositorios JPA do Config:
workspace e cenario escopados ancoram o teste, `DomainRuleTestRunService.record` persiste o run e o
resultado enriquecido, e `list` rele a mesma evidencia operacional. O gate confirma exatamente um
run/resultado e encerra o PostgreSQL descartavel ao final.
