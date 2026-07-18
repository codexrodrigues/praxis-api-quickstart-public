# Rule Lab QL-08 — runbook do drill distribuído de outbox

## Objetivo e limite da evidência

Este drill valida a fronteira operacional do P2F-ADR-10 sem executar regra Ergon nem promover
autoridade. Quickstart e consumidor rodam em JVMs distintas, comunicam-se somente por HTTPS e
persistem em branches efêmeras de dois projetos PostgreSQL Neon. A garantia exercitada é
`commit local + at-least-once + inbox idempotente + reconciliação`; não há claim de exactly-once
distribuído.

O harness é opt-in, não adiciona endpoint, scheduler ou credencial fixa ao produto e nunca imprime
os valores lidos do `.env.dev`. A branch Neon é a unidade de isolamento do datasource da API porque
as entidades existentes do Quickstart fixam `schema = "public"`. O consumidor usa um schema dedicado
na segunda branch. Não execute o drill contra endpoints da branch principal.

## Pré-requisitos e preparação no Neon

- Windows PowerShell e JDK 21 em `JAVA_HOME`;
- `.env.dev` privado com `SPRING_DATASOURCE_*` e `CONFIG_DATASOURCE_*` apontando para Neon com
  `sslmode=require`;
- duas branches **schema-only**, efêmeras e com expiração configurada, uma em cada projeto;
- na branch da API, `GRANT USAGE, CREATE ON SCHEMA public TO <SPRING_DATASOURCE_USERNAME>`;
- na branch do consumidor, schema `ql08_drill_consumer_20260715` criado pelo owner e acessível ao
  usuário de `CONFIG_DATASOURCE_USERNAME`.

Essas alterações devem existir somente nas branches efêmeras. O datasource de configuração do host
usa o `public` clonado da branch do segundo projeto; o inbox externo permanece no schema dedicado.
Depois do ensaio, exclua as duas branches pelo Neon, mesmo que tenham auto-delete configurado.

## Execução

Na raiz do `praxis-api-quickstart`:

```powershell
$env:JAVA_HOME='C:\Users\rodrigo.moreira\.jdks\openjdk-21.0.2'
powershell -ExecutionPolicy Bypass -File scripts/workspace/Invoke-RuleLabQl08DistributedOutboxDrill.ps1 `
  -ApiBranchHost '<endpoint-pooler-da-branch-api>.neon.tech' `
  -ConsumerBranchHost '<endpoint-pooler-da-branch-consumidor>.neon.tech' `
  -EphemeralBranches
```

O script rejeita hosts fora do Neon ou iguais aos endpoints canônicos. Ele:

1. deriva URLs das branches trocando apenas o hostname das URLs governadas no `.env.dev`;
2. gera certificado servidor e truststore temporários com SAN `localhost`/`127.0.0.1`;
3. inicia um consumidor HTTPS em outra JVM e persiste o inbox no segundo Neon;
4. força timeout de 750 ms depois do primeiro commit externo e exige `RETRY_SCHEDULED`;
5. reinicia consumidor e Quickstart, consulta o acknowledgement e exige `RECONCILED` sem redelivery;
6. rotaciona o bearer, exige HTTP 401 para a credencial anterior e `DELIVERED` para a nova;
7. encerra as JVMs e remove keystore, certificado, truststore e variáveis temporárias.

Timeouts de reconciliação e entrega normal são de 10 s para absorver cold start do Neon. O timeout
curto existe somente na fase ambígua deliberada.

## Evidência esperada

O sucesso produz
`target/rule-lab-ql08-distributed-drill/distributed-outbox-drill-evidence.json`, sem URL, senha,
token, certificado ou payload de negócio. Valores obrigatórios:

- `ambiguousDelivery=RETRY_SCHEDULED`;
- `restartReconciliation=RECONCILED`;
- `oldCredentialHttpStatus=401`;
- `rotatedDelivery=DELIVERED`;
- `consumerInboxRows=2`;
- `transport=HTTPS`;
- `databaseIsolation=TWO_EPHEMERAL_NEON_BRANCHES`;
- `processBoundary=SEPARATE_JVM`.

## Diagnóstico e recuperação

| Sintoma | Causa provável | Ação segura |
| --- | --- | --- |
| host recusado pelo script | endpoint canônico, não-Neon ou formato incorreto | informar somente o hostname da branch efêmera |
| `permission denied for schema public` | usuário da API não recebeu DDL na branch | conceder `USAGE, CREATE` somente no `public` da branch efêmera |
| consumidor não cria `.ready` | TLS, JDBC ou schema do inbox | corrigir a branch; não apontar para produção |
| probe/POST expira na fase normal | cold start do compute/DB Neon | manter 10 s; não confundir com o timeout deliberado de 750 ms |
| primeira entrega retorna `DELIVERED` | falha ambígua não foi exercitada | invalidar a execução e revisar a precedência do timeout |
| mais de uma linha por `messageId` | deduplicação/fingerprint quebrado | bloquear promoção e preservar os bancos para análise |
| credencial antiga não retorna 401 | rotação não aplicada ao processo reiniciado | bloquear o drill |
| `HTTP_AUTHORIZATION_REJECTED` | token revogado, audience/role incorreto ou política do destino | não repetir automaticamente; corrigir a credencial e governar o replay |
| `HTTP_CONTRACT_REJECTED` ou `HTTP_ACK_*` | endpoint ou acknowledgement incompatível | bloquear integração e corrigir o contrato antes do replay |
| `HTTP_THROTTLED` ou `HTTP_TARGET_UNAVAILABLE` | limite ou indisponibilidade transitória | acompanhar backlog/SLO; preservar retry limitado e reconciliação |
| replay retorna `REJECTED_QUARANTINE` | efeito anterior ainda pode estar em voo | aguardar a janela configurada; não reduzir a quarentena para forçar passagem |
| replay retorna `REJECTED_FAILURE_CHANGED` | visão administrativa ficou obsoleta | recarregar estado e reiniciar a análise com o código atual |
| replay retorna `ACKNOWLEDGED_NO_REPLAY` | consumidor já persistiu o efeito | encerrar incidente sem nova entrega e preservar a auditoria |

## Resultado desta revisão

Execução concluída em 2026-07-15 contra duas branches schema-only efêmeras do Neon. O JSON registrou
`RETRY_SCHEDULED`, `RECONCILED`, HTTP 401 para a credencial anterior, `DELIVERED` para a credencial
nova e duas linhas finais no inbox. As duas branches foram excluídas imediatamente após a coleta da
evidência.

## Critérios adicionais para uso corporativo

Este drill é pré-requisito, não substituto, para o ambiente-alvo. Antes de efeito autoritativo ainda
são obrigatórios secret manager, certificado de PKI corporativa, retenção de inbox/auditoria,
métricas e alertas, ownership do job, SLO, procedimento de dead-letter, recovery drill sob
indisponibilidade real e aprovação do sistema consumidor.

O contrato operacional, as métricas, a retenção segura e os critérios para selecionar o adapter
real estão em [`RULE-LAB-QL-08-CORPORATE-OPERATIONS.md`](RULE-LAB-QL-08-CORPORATE-OPERATIONS.md).
