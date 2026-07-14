# Rule Lab QL-05 — persistência, concorrência e efeitos

## Resultado

QL-05 promove `human-resources.extraordinary-benefit-requests` de action de avaliação para recurso
persistente read-only com mutações exclusivamente governadas. O engine continua puro: decide e
produz `EffectIntent`; o host Quickstart possui persistência, transações, identidade, autorização e
execução do efeito.

## Semântica de negócio

```text
evaluate(ALLOW) -> EVALUATED -> submit -> SUBMITTED -> approve -> APPROVED -> apply -> APPLIED
evaluate(DENY | INCONCLUSIVE | NOT_APPLICABLE | ERROR) -> nenhum recurso, nenhuma transição, nenhum efeito
```

- A referência externa é única e preserva vínculo com o atendimento de origem.
- A evidência persistida inclui snapshot, revisão de ativação, hashes de conteúdo/fatos/plano,
  regra e versão usadas na decisão.
- Consultas e filtros não reavaliam regras; reconstroem a decisão registrada.
- Não há `create`, `update` ou `delete` público que permita saltar estados.

## Concorrência e idempotência

Actions item-level exigem `If-Match`; ausência retorna `428`, versão obsoleta retorna `412`. O ETag
é derivado da versão JPA persistida e nunca de timestamp ou payload.

`Idempotency-Key` é escopado por `resourceKey`, item (ou coleção), action e ator autenticado. O hash
canônico do comando impede reutilizar a mesma chave com payload diferente. O replay é consultado
antes do ETag, permitindo que uma repetição de uma action já concluída devolva o resultado original
mesmo que o recurso já tenha avançado de versão.

Para comandos unitários, mutação do agregado, transição auditável e conclusão idempotente formam
uma transação. A reserva `STARTED` é confirmada antes, de modo que concorrentes observem execução em
andamento; se a transação falhar, nenhuma mutação parcial sobrevive e a execução fica `FAILED` com
diagnóstico limitado ao tamanho do contrato físico.

## Lote

`evaluate-batch` aceita de 1 a 50 solicitações, preserva a ordem e declara `atomic=false`. Cada item
elegível possui transação independente. O retorno diferencia `PERSISTED`, decisões do engine,
`DUPLICATE_REFERENCE` e `INVALID_TIME_ZONE`, sem desfazer sucessos anteriores. O lote nunca executa
efeitos.

## Efeito e fronteira corporativa

`apply` grava uma única linha em `extraordinary_benefit_grant_effect` e avança o agregado para
`APPLIED` na mesma transação. Restrições únicas por solicitação e por `effect_execution_id` impedem
duplicação. Uma falha no ledger reverte status, versão e transição.

Este ledger demonstra atomicidade apenas porque agregado e efeito estão no mesmo datasource. Uma
integração real com folha, ERP ou banco externo não deve ocorrer dentro da transação HTTP. A evolução
corporativa correta é gravar um outbox no mesmo commit e entregar externamente com consumidor
idempotente, retries, dead-letter e reconciliação operacional.

## Superfícies públicas

- `GET /api/human-resources/extraordinary-benefit-requests`
- `GET /api/human-resources/extraordinary-benefit-requests/{id}`
- `POST /api/human-resources/extraordinary-benefit-requests/actions/evaluate`
- `POST /api/human-resources/extraordinary-benefit-requests/actions/evaluate-batch`
- `POST /api/human-resources/extraordinary-benefit-requests/{id}/actions/submit`
- `POST /api/human-resources/extraordinary-benefit-requests/{id}/actions/approve`
- `POST /api/human-resources/extraordinary-benefit-requests/{id}/actions/apply`
- `/schemas/filtered`, `/schemas/actions`, `/schemas/surfaces` e capabilities contextuais continuam
  derivados do contrato canônico do metadata starter.

## Evidência automatizada

`ExtraordinaryBenefitRequestPilotIntegrationTest` prova por HTTP autenticado:

- persistência exclusiva de `ALLOW` e ausência de efeito na avaliação;
- replay e conflito de fingerprint idempotente;
- discovery/schema e ausência de CRUD mutável;
- `428` sem ETag e `412` com ETag obsoleto;
- lifecycle completo e efeito exatamente uma vez;
- rollback de `apply` quando o ledger conflita;
- lote misto com ordem, falha parcial e uma transação por item;
- negação sem persistência e rejeição de ator não autenticado.

O schema PostgreSQL está em `V20260713_001` e `V20260713_002`; o drift check cobre o novo escopo de
idempotência, o agregado e o ledger.

## Próximo gate

QL-06 deve adicionar shadow/dual-run observável entre regra legada e snapshot Praxis, sem permitir
que divergências executem efeitos. A comparação deve usar os mesmos fatos congelados e produzir
métricas, amostras sanitizadas e critério explícito de promoção/rollback.
