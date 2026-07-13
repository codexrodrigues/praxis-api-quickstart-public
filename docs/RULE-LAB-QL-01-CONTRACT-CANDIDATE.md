# Laboratório de regras — QL-01 Contract Candidate

- Data-base: 2026-07-13
- Caso: Concessão de Benefício Corporativo Extraordinário
- Estado: `CONTRACT_READY candidate`; não é API pública aprovada
- Predecessor: `RULE-LAB-QL-00-ADHERENCE-INVENTORY.md`
- Regra de parada: nenhum tipo desta especificação deve ser criado no pacote do
  Quickstart antes do P2F-ADR-01

## 1. Boundary e identidade

```text
domainKey: workforce-benefits
boundedContextKey: extraordinary-assistance
ruleSetKey: extraordinary-grant-eligibility
operationKey: evaluate-extraordinary-grant
resourceKey candidate: human-resources.extraordinary-benefit-requests
snapshotVersion: immutable positive integer
```

Identidades não usam path HTTP, classe, tabela, package, HADES, `ROWID`, empresa
ou sessão.

## 2. Lifecycles separados

### 2.1 Solicitação de negócio

```text
DRAFT -> EVALUATED -> SUBMITTED -> APPROVED -> APPLIED
                              \-> REJECTED
```

- `EVALUATED` registra snapshot e digest dos facts, não autorização permanente;
- `SUBMITTED` exige avaliação compatível e ETag da solicitação;
- `APPROVED` exige alçada e idempotency key;
- `APPLIED` só existe após effects transacionais;
- mudança de facts ou snapshot exige nova avaliação quando afetar o digest.

### 2.2 Definition/materialization

O laboratório consome o lifecycle canônico do Config Starter. Definition,
materialization e solicitação são entidades distintas. Authoring simulation não
move a solicitação para `EVALUATED`.

### 2.3 Snapshot candidate

```text
DRAFT -> VALIDATED -> APPROVED -> PUBLISHED -> ACTIVE -> SUPERSEDED | REVOKED
```

Somente `ACTIVE` pode ser selecionado. Falha ao carregar nova versão mantém a
anterior; não existe estado parcialmente ativo.

## 3. Facts candidate

| Root | Obrigatoriedade | Freshness | Conteúdo permitido |
| --- | --- | --- | --- |
| `request` | obrigatório | versão da solicitação | motivo codificado, data, valor, moeda e correlação |
| `actor` | obrigatório | por requisição | permissões materializadas e escopo |
| `worker` | obrigatório | conforme política | status, vínculo, país/localidade e datas mínimas |
| `program` | obrigatório | versão do snapshot | vigência, motivos, faixas e limites oficiais |
| `customerPolicy` | opcional por binding | versão publicada | restrições, parâmetros e calendário permitido |
| `priorGrants` | obrigatório | instante declarado | agregados e conflitos, sem histórico bruto |
| `payrollContext` | obrigatório | competência corrente | fechamento, moeda, `nowUtc` e timezone |
| `budgetContext` | obrigatório no slot | TTL explícito | disponibilidade e `resolvedAt` |
| `provenance` | obrigatório | junto ao grupo | source key, version, resolvedAt e freshness |

`missing`, `null`, vazio, stale e access-denied são estados diferentes. O host
monta facts antes da avaliação; bindings não fazem I/O.

Fixture mínima:

```json
{
  "request": {
    "reasonCode": "RELOCATION",
    "eventDate": "2026-07-10",
    "requestedAmount": 2500.00,
    "currency": "BRL"
  },
  "actor": { "permissions": ["benefit:request"], "scope": "BR-SP" },
  "worker": {
    "status": "ACTIVE",
    "employmentType": "EMPLOYEE",
    "country": "BR",
    "location": "SP"
  },
  "program": { "active": true, "maxAmount": 5000.00 },
  "customerPolicy": { "maxAmount": 3000.00, "minimumTenureDays": 90 },
  "priorGrants": { "conflictingCount": 0, "rolling12MonthAmount": 0.00 },
  "payrollContext": {
    "competence": "2026-08",
    "closingState": "OPEN",
    "functionalCurrency": "BRL",
    "nowUtc": "2026-07-13T15:00:00Z",
    "userTimeZone": "America/Sao_Paulo"
  },
  "budgetContext": {
    "availableAmount": 100000.00,
    "currency": "BRL",
    "resolvedAt": "2026-07-13T14:59:30Z"
  }
}
```

## 4. Decision slots candidate

| Ordem | Slot | Stage | Executor | Source | Composição | Override do produto |
| --- | --- | --- | --- | --- | --- | --- |
| 10 | `request.authorization-integrity` | `PROTECTED_GUARD` | Java | `SECURITY` | — | `FORBIDDEN` |
| 20 | `worker.legal-eligibility` | `PROTECTED_GUARD` | JSON Logic | `PRODUCT` | — | `FORBIDDEN` |
| 30 | `grant.duplicate-conflict` | `PROTECTED_GUARD` | Java | `PRODUCT` | — | `FORBIDDEN` |
| 40 | `program.applicability` | `DOMAIN_DECISION` | JSON Logic | `PRODUCT` | `RESTRICT` | `RESTRICT_ONLY` |
| 50 | `customer.additional-eligibility` | `DOMAIN_DECISION` | JSON Logic | `CUSTOMER` | `RESTRICT` | `RESTRICT_ONLY` |
| 60 | `payment.calendar-policy` | `DOMAIN_DECISION` | JSON Logic | `PRODUCT/CUSTOMER` | `REPLACE_EXACT` | `REPLACEABLE` |
| 70 | `grant.amount-parameters` | `DOMAIN_DECISION` | parameters | `PRODUCT/CUSTOMER` | `PARAMETERIZE` | `PARAMETERIZABLE` |
| 80 | `grant.amount-calculation` | `DOMAIN_DECISION` | Java | `PRODUCT` | — | `FORBIDDEN` |
| 90 | `budget.availability` | `POST_DECISION` | JSON Logic | `PRODUCT` | `RESTRICT` | `RESTRICT_ONLY` |
| 100 | `grant.effect-plan` | `EFFECT_INTENT` | host | `GENERATED_INFRASTRUCTURE` | — | `FORBIDDEN` |

Composição descreve como bindings participam da decisão. Override do produto
limita o que o tenant pode configurar no slot. Os dois vocabulários não são
intercambiáveis.

Dependências são explícitas. O número é desempate; publicação resolve
topological sort e rejeita ciclo, referência ausente, cross-boundary e empate.

## 5. Consolidação

- protected `DENY` encerra a avaliação;
- `RESTRICT` usa deny-overrides;
- `NOT_APPLICABLE` não equivale a `ALLOW`;
- missing/stale obrigatório produz `INCONCLUSIVE`, salvo fail policy mais
  restritiva aprovada;
- implementação, snapshot ou operador incompatível produz `TECHNICAL_ERROR`,
  nunca negativa de negócio;
- cálculo não executa após negativa;
- effects só são planejados após `ALLOW` consolidado.

## 6. RuleEvaluationResult candidate

| Campo | Semântica |
| --- | --- |
| `decision` | enum consolidado, nunca boolean implícito |
| `ruleSetRef`/`snapshotRef` | identidade e versão avaliadas |
| `bindingResults` | decisão, reason codes, duração e diagnostics por binding |
| `calculation` | valor, moeda, competência e arredondamento quando aplicável |
| `effectIntents` | intenções tipadas; nunca efeitos executados |
| `observationRef` | referência redigida, sem facts integrais |
| `factsDigest` | digest canônico para compatibilidade/replay controlado |

Reason codes iniciais:

```text
REQUEST_NOT_AUTHORIZED
WORKER_NOT_ELIGIBLE
DUPLICATE_GRANT
PROGRAM_NOT_APPLICABLE
CUSTOMER_POLICY_RESTRICTED
BUDGET_INSUFFICIENT
FACT_REQUIRED_MISSING
FACT_STALE
SNAPSHOT_INCOMPATIBLE
IMPLEMENTATION_UNAVAILABLE
EVALUATION_LIMIT_EXCEEDED
```

## 7. Matriz de erro e fail policy

| Camada | Condição | Resultado/comportamento |
| --- | --- | --- |
| Authoring | schema/owner/tenant ausente | não publica |
| Review | protected override ou aprovação inválida | publicação bloqueada |
| Publication | ciclo, target ausente, incompatibilidade ou ETag divergente | nenhum snapshot novo |
| Load | hash, tenant, environment ou versão incompatível | snapshot anterior permanece |
| Load | implementação Java ausente | snapshot rejeitado; binding não é pulado |
| Facts | obrigatório ausente | `INCONCLUSIVE/FACT_REQUIRED_MISSING` |
| Facts | `null` permitido | regra avalia `null` explicitamente |
| Facts | stale | `INCONCLUSIVE/FACT_STALE` |
| Evaluation | condição de negócio falha | `DENY` com reason code de domínio |
| Evaluation | limite/timeout/operador falha | `TECHNICAL_ERROR` |
| Shadow | baseline indisponível | comparação inconclusiva; sem mutação |
| Command | ETag divergiu | conflito; reavaliar |
| Command | idempotency key com payload distinto | conflito; sem replay parcial |
| Effect | persistência downstream falha | rollback da unidade transacional |
| Observation | redaction falha | observação reduzida/bloqueada; facts não vazam |

HTTP status e envelope serão decididos em QL-04, alinhados ao contrato canônico
do host. Status HTTP isolado não prova equivalência.

## 8. EffectIntent candidate

- `REGISTER_EXTRAORDINARY_GRANT`;
- `RESERVE_BUDGET`;
- `REQUEST_HUMAN_APPROVAL`;
- `SCHEDULE_PAYROLL_EVENT`;
- `RECORD_AUDIT`;
- `NOTIFY_REQUESTER`.

Cada intent contém identity/idempotency key, preconditions e payload mínimo. O
engine não executa intent; o application service define transação e rollback.

## 9. Modos e autoridade

- `RuleExecutionMode`: `TEST`, `SHADOW`, `PREFLIGHT`, `AUTHORITY`;
- `RuleAuthority`: `REFERENCE_ONLY`, `CAN_BLOCK_PROVEN_DENY`, `AUTHORITATIVE`.

`TEST` e `SHADOW` vêm primeiro. `PREFLIGHT` e `AUTHORITY` exigem gates próprios
e não são ativados por property ou label improvisado.

## 10. Goldens mínimos antes de QL-02

1. ALLOW determinístico;
2. protected DENY com short-circuit;
3. restrição do cliente sem ampliação;
4. replacement somente no calendário;
5. parâmetro dentro/fora da faixa;
6. cálculo Java com moeda e arredondamento;
7. missing, `null` e stale distintos;
8. ciclo e binding ausente bloqueados;
9. tenant incompatível bloqueado;
10. implementação Java ausente bloqueada;
11. hot reload sem mistura de versões;
12. shadow mismatch sem mutação;
13. lote com resultado por item;
14. rollback sem dupla mutação;
15. observation redigida.

## 11. Gate QL-01

QL-01 está completo como candidato documental. QL-02 não está autorizado
enquanto:

- P2F-ADR-01 não definir o owner Java mínimo;
- P2F-ADR-03/04 não aprovarem composição, stages e DAG;
- P2F-ADR-06 não confirmar o dialect JSON Logic;
- P2F-ADR-08 não aprovar result/error/fail policy;
- os goldens não tiverem schema/checker acordado.

Não há endpoint, tabela, migration, DTO público ou Fase 9 criado por este
documento.
