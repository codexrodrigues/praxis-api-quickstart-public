# Rule Lab QL-08 — contrato operacional para integração corporativa

## Decisão e fronteira

O engine continua puro e produz somente `EffectIntent`. O host é responsável por transação local,
outbox, adapter, autenticação, retries, reconciliação e retenção. O sistema consumidor é responsável
pelo inbox durável, deduplicação por `messageId` e consulta independente do acknowledgement.

Este pacote ainda não seleciona um produto, broker ou endpoint Ergon. Ele prepara a fronteira que o
adapter real deve implementar sem transformar uma configuração de laboratório em contrato
corporativo implícito.

## Fundação FND-06 — facts autoritativos server-side

O adapter opt-in `ExtraordinaryBenefitFactProvider` separa os dados legitimamente comandados pelo
caller dos facts de elegibilidade pertencentes ao host. A consulta usa o datasource operacional em
transação `read-only` com isolamento `REPEATABLE_READ` e exige correspondência exata de tenant,
ambiente, organização, referência opaca, versão e janela `[effectiveFrom, effectiveTo)`.

A ausência de snapshot e a coexistência de duas versões efetivas produzem falhas fechadas distintas.
A proveniência contém somente provider key, sistema de origem allowlisted, versão, instante da fonte,
`asOf`, digest do registro e digest HMAC do escopo. Tenant, organização, referência e valores de negócio
não aparecem na observação. A decisão e a proveniência viajam juntas no resultado interno, preservando
a versão exata usada. A migration revoga `PUBLIC`; o deploy concede apenas `SELECT` ao papel runtime
concreto do ambiente. As migrations operacionais
`V20260716_001` e `V20260716_002` instalam o schema e uma fixture explicitamente fictícia.

Esta fundação permanece desabilitada por padrão por
`PRAXIS_RULE_LAB_AUTHORITATIVE_FACTS_ENABLED=false`; nesse estado o endpoint persistente falha
fechado. Ela não substitui o baseline independente, não executa shadow, não publica regra Ergon e
não altera o gate global da Parte 2.
Quando habilitada, exige `PRAXIS_RULE_LAB_FACT_SCOPE_HMAC_KEY` com no mínimo 32 bytes; a chave deve
vir do secret manager do ambiente, ser rotacionável e nunca aparecer em log ou no repositório.
Seu resultado é `FND-06_REFERENCE_ADAPTER_IMPLEMENTED`, não `Shadow Running`, `Shadow Passed` ou
`Ready for Rule Inventory`.

## Atualizacao FND-06: endpoint e revalidacao antes do efeito

O corte atual substitui a limitacao descrita acima: o endpoint persistente aceita somente os dados
comandaveis e uma `factReference`; estados funcionais, limites, calendario e saldo nao pertencem ao
schema do caller. `evaluate` persiste a provenance inicial. `apply` recaptura os facts e reexecuta o
snapshot imediatamente antes de inserir o ledger, na mesma transacao. Divergencia de elegibilidade,
valor, plano ou snapshot retorna 412, mantem `APPROVED` e nao cria efeito.

As migrations `V20260716_001`, `V20260716_002` e `V20260716_004` instalam fonte, fixture ficticia e
evidencia da revalidacao. O resultado e `FND-06_HOST_FACTS_AND_APPLY_REVALIDATION_PROVED`; nao e
`Shadow Running`, `Shadow Passed` nem `Ready for Rule Inventory`.

O proof interno QL-08 `STATEMENT_ATOMIC` ainda usa facts congelados. Suas linhas nao possuem
provenance e sao inelegiveis para `apply`. Esse boundary deve ser removido quando o statement proof
migrar para aquisicao autoritativa coordenada.

## SPI do job governado

O deployment deve fornecer um scheduler/job com ownership explícito e invocar:

- `ExtraordinaryBenefitStatementOutboxDispatcher.dispatchNext()` até receber `EMPTY` ou alcançar o
  limite de trabalho da execução;
- `ExtraordinaryBenefitStatementOutboxReconciler.reconcileNext()` para resultados ambíguos;
- `ExtraordinaryBenefitStatementOutboxOperations.snapshot()` para backlog e SLO;
- `ExtraordinaryBenefitStatementOutboxOperations.purgeDelivered()` somente no job de retenção.

O Quickstart deliberadamente não inicia scheduler próprio. Isso impede concorrência e capacidade
operacional implícitas em cada réplica HTTP.

## Métricas

Todas as tags têm cardinalidade fechada e não incluem tenant, mensagem, correlação ou payload:

| Métrica Micrometer | Tag | Uso |
| --- | --- | --- |
| `praxis.rule.lab.outbox.dispatches` | `outcome` | volume de `DELIVERED`, `RETRY_SCHEDULED`, `DEAD_LETTERED`, `EMPTY` e `NO_SINK` |
| `praxis.rule.lab.outbox.dispatch.duration` | `outcome` | latência do ciclo de dispatch, incluindo claim e acknowledgement |
| `praxis.rule.lab.outbox.reconciliations` | `outcome` | resultado dos scans de reconciliação |
| `praxis.rule.lab.outbox.reconciliation.duration` | `outcome` | latência do ciclo de reconciliação |
| `praxis.rule.lab.outbox.retention.deleted` | nenhuma | linhas `DELIVERED` removidas pelo job de retenção |
| `praxis.rule.lab.outbox.replays` | `outcome` | decisões bounded da autorização governada de replay |

O snapshot operacional traz apenas instante da observação, contagens por estado e criação do item
não entregue mais antigo. Ele não expõe IDs ou dados de negócio.

## Classificação segura de falhas do adapter HTTP

O adapter não trata toda resposta externa como indisponibilidade. A classificação é fechada,
persistida apenas como código seguro e determina se ainda existe chance legítima de retry:

| Condição | Código operacional | Decisão |
| --- | --- | --- |
| HTTP `401` ou `403` | `HTTP_AUTHORIZATION_REJECTED` | permanente; dead-letter imediato |
| HTTP `408`, `425` ou `5xx` | `HTTP_TARGET_UNAVAILABLE` | transitória; retry limitado |
| HTTP `429` | `HTTP_THROTTLED` | transitória; retry limitado |
| demais respostas não `2xx` | `HTTP_CONTRACT_REJECTED` | permanente; dead-letter imediato |
| timeout ou falha de transporte | `HTTP_TIMEOUT` / `HTTP_TRANSPORT_UNAVAILABLE` | transitória; retry limitado |
| acknowledgement inválido, incompatível ou excessivo | `HTTP_ACK_*` | permanente; dead-letter imediato |

Corpo de erro, URL, token e dados de negócio não são gravados na outbox. Falha permanente não
consome artificialmente todas as tentativas antes de alertar o operador. A classificação não
substitui a tabela de erros que deverá ser acordada com o sistema corporativo real.
Mesmo em dead-letter, o probe independente continua apto a reconciliar um acknowledgement que já
tenha sido persistido pelo consumidor; o estado terminal bloqueia redelivery cego, não a conciliação.

## Política de retenção

- padrão: reter `DELIVERED` por 30 dias;
- cada passagem remove no máximo 500 linhas;
- `PENDING`, `PROCESSING` e `DEAD_LETTER` nunca são removidos por esse serviço;
- o delete repete status e cutoff na cláusula de remoção para não confiar apenas na seleção anterior;
- propriedades: `PRAXIS_RULE_LAB_OUTBOX_RETENTION_DELIVERED_DAYS` (1–3650) e
  `PRAXIS_RULE_LAB_OUTBOX_RETENTION_BATCH_SIZE` (1–10000).

## Replay governado de dead-letter

O Quickstart fornece `ExtraordinaryBenefitStatementReplayService.requestReplay(...)` como SPI
interna, sem endpoint administrativo e sem replay automático. O comando exige `messageId`, código de
falha observado, ator, justificativa com pelo menos dez caracteres e correlação. Cada decisão gera
uma linha append-only em `extraordinary_benefit_statement_replay_audit`, sem payload ou resposta
externa.

O replay somente é agendado quando:

1. a mensagem ainda está em `DEAD_LETTER` sob lock pessimista;
2. o código de falha continua igual ao observado pelo operador;
3. a quarentena desde a última falha expirou;
4. existe probe independente e ele confirma ausência de acknowledgement.

Se o probe encontrar acknowledgement, a mensagem vira `DELIVERED` e não é reenviada. Falha, ausência
ou resposta inconsistente do probe bloqueia o replay. Um replay autorizado zera apenas o contador do
novo ciclo de entrega; a decisão anterior permanece na auditoria. A quarentena padrão é cinco minutos
e usa `PRAXIS_RULE_LAB_OUTBOX_REPLAY_QUARANTINE_MS` (1 segundo a 7 dias).

Essa proteção reduz a janela de efeito externo ainda em voo, mas não promete atomicidade distribuída.
O sistema corporativo real ainda deve oferecer inbox idempotente e consulta autoritativa.

## Auditoria append-only de transformações

Toda transformação tipada efetivamente materializada gera uma linha em
`extraordinary_benefit_transformation_audit` na mesma transação da solicitação. A linha contém a
identidade técnica completa da proposta, coordenadas do snapshot e do RuleSet, digests de facts e
plano, correlação e os digests canônicos dos envelopes `before` e `after` publicados pelo
engine.

O valor monetário, a referência da solicitação, os facts e qualquer payload JSON não são
persistidos na auditoria. A API de persistência expõe somente inserção, a entidade é imutável e a
migration PostgreSQL instala trigger que rejeita `UPDATE` e `DELETE` diretos. Replay idempotente reutiliza
o resultado existente sem nova linha; falha antes do commit reverte solicitação, auditoria e outbox
como uma única unidade.

O ator não é duplicado nessa tabela: a correlação referencia o ledger idempotente protegido, que é
o owner da identidade do comando. Isso reduz exposição sem perder accountability operacional.

Digests permitem correlação redigida, mas não são criptografia nem anonimização. O acesso à tabela
continua restrito ao papel operacional de auditoria.

P2F-ADR-12 adiciona uma exceção explícita e segregada à imutabilidade: somente a função SQL de
retenção pode remover lotes vencidos, sempre com política, cutoff, UUID, key id versionado e HMAC da autorização. Cada
execução gera trilha append-only; legal hold ativo exclui o registro. A aplicação não recebe
`DELETE`, não chama essas funções e não expõe endpoint administrativo. O contrato operacional
completo está em `RULE-LAB-P2F-ADR-12-PRIVACY-RETENTION.md`.

A corrida entre legal hold e retenção também foi exercitada com duas conexões
PostgreSQL reais numa branch Neon efêmera em 2026-07-15. Os dois vencedores
possíveis preservaram atomicidade e não produziram hold nem evento órfão. Essa
prova técnica não substitui provisionamento corporativo de papéis distintos,
secret manager e rotação reais, scheduler, observabilidade, backup/restore e aprovação da
política.

O laboratório também inclui um template parametrizado de grants e uma prova de
quatro capability roles `NOLOGIN`. Em Neon, os caminhos permitidos passaram e
dez escaladas cruzadas falharam com `insufficient_privilege`; isso valida a
matriz, mas não afirma que workload identities ou grants já foram implantados
no ambiente corporativo-alvo.

O contrato HMAC do laboratório agora fixa canonicalização com separação de
domínio e propósito, mínimo de 256 bits e key id opaco. A migration `V20260715_004`
remove as funções sem key id, e a prova de papéis usa HMAC sintético real para
confirmar rotação e provenance sem persistir segredo ou referência externa.
Isso valida o contrato técnico, não a integração com o secret manager real.

## SLO inicial para o piloto corporativo

Os valores abaixo são critérios de entrada do piloto, não promessa universal da plataforma:

- 99% das mensagens aceitas localmente entregues ou reconciliadas em até 5 minutos;
- nenhuma mensagem `PENDING` com idade superior a 15 minutos sem alerta;
- qualquer `DEAD_LETTER` abre alerta de severidade alta;
- taxa de `DEAD_LETTERED` inferior a 0,1% em janela de 24 horas;
- disponibilidade do probe de acknowledgement acompanhada separadamente do POST;
- ausência de consumo do job deve ser detectada por heartbeat da plataforma de jobs, não por um
  scheduler oculto no Quickstart.

## Critérios para escolher e aceitar o adapter real

Antes de codificar o adapter específico, registrar owner, sistema alvo, endpoint/protocolo, PKI,
secret manager, limites, timeout, política de retry, contrato de idempotência, retenção do inbox,
consulta de acknowledgement, classificação de erros e ambiente de teste. O aceite exige:

1. teste de contrato positivo e negativo sem dados reais;
2. commit externo seguido de perda de resposta e reconciliação sem segundo efeito;
3. restart de produtor e consumidor;
4. rotação/revogação de credencial;
5. indisponibilidade acima da janela de retry e dead-letter observável;
6. restauração do serviço e replay governado com auditoria;
7. evidência de dashboards, alertas, retenção e runbook aprovados pelos owners.

Até esses itens terem owner e evidência, o adapter HTTPS do laboratório não deve ser apontado para
um sistema corporativo nem tratado como autorização para Fase 9.
