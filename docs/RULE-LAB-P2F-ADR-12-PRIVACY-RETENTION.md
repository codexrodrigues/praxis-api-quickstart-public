# Rule Lab — P2F-ADR-12 privacy, legal hold e retenção

## Estado e fronteira

Esta prova aplica P2F-ADR-12 aos ledgers neutros do QL-08. Ela não cria endpoint
administrativo e não move observação, duração, persistência ou política de
retenção para o `praxis-rules-engine`.

O resultado determinístico continua sendo `RuleEvaluationResult`. O host
publica apenas a allowlist sanitizada do QL-06 e mantém duas trilhas duráveis:

| Ledger | Conteúdo protegido | Conteúdo proibido |
| --- | --- | --- |
| `extraordinary_benefit_statement_replay_audit` | decisão administrativa, reason codes técnicos, correlação e identidade do operador | payload/response externa, facts, tokens e secrets |
| `extraordinary_benefit_transformation_audit` | coordenadas técnicas, snapshot/RuleSet e digests canônicos | valores before/after, referência de negócio, facts, payload e ator duplicado |

Digest não é anonimização. `authorization_reference_digest` deve ser um
HMAC-SHA-256 uppercase calculado pelo control plane corporativo com segredo fora
do banco. Não use SHA-256 simples sobre usuário, ticket ou chamado.

O contrato canônico do laboratório usa domínio
`PRAXIS-RULE-AUDIT-AUTHORIZATION-V1` e campos UTF-8 prefixados pelo respectivo
tamanho para vincular `authorization_key_id`, propósito (`LEGAL_HOLD` ou
`RETENTION`) e referência externa sem ambiguidade. A chave deve ter ao menos
256 bits. `authorization_key_id` é um alias opaco e versionado; não pode revelar
path, ARN, vault, projeto ou nome do segredo. O banco persiste apenas key id e
digest, nunca a referência externa ou o segredo.

`V20260715_004__rule_audit_authorization_key_rotation.sql` adiciona essa
provenance aos três registros governados e remove as assinaturas antigas sem
key id. Evidência anterior é preservada honestamente como
`LEGACY-UNVERSIONED`. Depois da migration, o template de grants deve ser
reaplicado porque as assinaturas das funções mudaram.

O harness de branch efêmera cria primeiro evidência pelas assinaturas antigas,
aplica `V20260715_004` e exige três registros retrocompatíveis marcados como
`LEGACY-UNVERSIONED`. Em seguida, confirma que as assinaturas sem key id não
existem mais. Assim, o gate cobre upgrade com dados append-only, não apenas
instalação vazia.

## Segregação de papéis

O deployment deve usar identidades distintas:

| Papel | Permissão mínima |
| --- | --- |
| migration owner | cria objetos e permanece somente como break-glass |
| application runtime | `INSERT` nos ledgers; sem `SELECT`, `UPDATE`, `DELETE` ou tabelas de controle |
| audit reader | `SELECT` estritamente necessário |
| compliance operator | `EXECUTE` somente em `record_rule_audit_legal_hold(...)` |
| retention worker | `EXECUTE` somente em `purge_rule_audit(...)` |

As funções são criadas sem `EXECUTE` para `PUBLIC`. A migration não inventa
nomes de roles: o provisionamento corporativo deve conceder os privilégios
acima e provar que aplicação, compliance e retenção não usam o owner.

O template operacional
`db/operational-provisioning/rule-audit-role-grants.sql.template` centraliza a
matriz de grants sem fixar nomes do ambiente. Os quatro placeholders devem ser
substituídos por identificadores SQL devidamente quoted por uma ferramenta de
deployment auditada. Prefira capability roles `NOLOGIN` e vincule a cada uma
uma workload identity distinta; não compartilhe a identidade do migration
owner.

## Legal hold

`record_rule_audit_legal_hold(...)` aceita somente os dois ledgers fechados,
UUID do evento/registro, ação `PLACE` ou `RELEASE`, reason code limitado, key id
versionado e HMAC da autorização externa. A tabela de estado contém apenas holds ativos; toda
colocação/liberação gera evento append-only.

`PLACE` bloqueia a linha-alvo antes de criar o estado. Assim, se compliance vencer
a corrida, o worker com `SKIP LOCKED` ignora o registro; se o expurgo vencer, o
hold falha sem criar estado órfão. Um registro sob hold é excluído da seleção de
retenção. Liberação inexistente, evento repetido, digest inválido ou ledger
desconhecido falham sem alteração.

## Expurgo governado

`purge_rule_audit(...)` exige:

- UUID novo por execução;
- ledger da allowlist;
- chave versionada da política aprovada;
- cutoff que preserve ao menos as últimas 24 horas;
- lote entre 1 e 10.000;
- key id versionado e HMAC da autorização externa.

Os candidatos são ordenados por instante/UUID e bloqueados com
`FOR UPDATE SKIP LOCKED`. `UPDATE` e `DELETE` diretos continuam rejeitados pelo
trigger, mesmo se um grant for concedido por engano. Um guard transacional
libera apenas o `DELETE` iniciado pela função governada. Hold, exclusão e
registro da execução participam da mesma transação; qualquer falha reverte tudo.

O registro de retenção guarda política, cutoff, batch, quantidade removida,
key id, HMAC da autorização e instante — nunca ator, ticket ou justificativa em claro.
Seu próprio ledger é append-only.

## Checklist de implantação

1. aplicar `V20260715_002__rule_audit_privacy_retention.sql` com migration owner;
2. revogar ownership e privilégios amplos da identidade de runtime;
3. conceder somente os grants da matriz de papéis;
4. executar o drift checker com credencial read-only;
5. provar `UPDATE` e `DELETE` diretos rejeitados;
6. provar hold preservado e registro não retido removido em lote;
7. provar liberação do hold e expurgo posterior;
8. provar UUID repetido, cutoff recente e digest inválido rejeitados;
9. **concluído em 2026-07-15:** provar em duas sessões concorrentes que `PLACE`
   e expurgo não criam hold órfão;
10. **concluído em laboratório em 2026-07-15:** provar canonicalização HMAC,
    isolamento por propósito, rotação de chave e persistência do key id;
11. encaminhar métricas/alertas do scheduler para a plataforma operacional;
12. registrar política, owner, legal basis, prazo e processo break-glass antes de uso corporativo.

Os itens 2–4 possuem agora uma prova laboratorial repetível, mas continuam
pendentes no ambiente corporativo real até existirem workload identities,
aprovação e evidência de deployment daquele ambiente.

## Prova de segregação de papéis

O runner abaixo aplica as migrations e o template numa branch Neon
`schema-only`, cria quatro capability roles `NOLOGIN` descartáveis e executa
sessões independentes com `SET ROLE`:

```powershell
$env:JAVA_HOME='D:\Developer\JAVA\openjdk-21_windows-x64_bin\jdk-21'
.\scripts\workspace\Invoke-RuleAuditRoleSeparationProof.ps1 `
  -BranchHost '<endpoint-da-branch>.neon.tech' `
  -EphemeralBranch `
  -ConnectionStringFromClipboard
```

O runner converte o hostname `-pooler` copiado do Neon para o endpoint direto
da mesma branch. `SET ROLE` é estado de sessão e não deve ser exercitado por um
pool transacional. Esse mecanismo existe apenas no harness: no ambiente-alvo,
cada workload identity deve autenticar no papel que lhe foi atribuído, sem
troca de papel numa conexão compartilhada.

A prova exige os caminhos positivos de append, leitura auditada, `PLACE` /
`RELEASE` e expurgo usando HMAC sintético real. Também exige rejeição SQLState `42501` para dez tentativas
de escalada cruzada, confirma que o reader não acessa o guard interno e que
nenhum capability role possui tabelas ou funções. Os papéis descartáveis são
removidos no `finally`; a evidência sanitizada é gravada em
`target/rule-audit-role-separation-proof/role-separation-evidence.json`.

Evidência coletada em 2026-07-15 numa branch Neon `schema-only` efêmera: o
harness recriou somente os objetos descartáveis de retenção, instalou a versão
anterior, semeou um hold, um evento de hold e uma execução de retenção sem key
id e confirmou o backfill dos três registros para `LEGACY-UNVERSIONED`. As
assinaturas antigas foram removidas e as versionadas instaladas. Os quatro
caminhos positivos passaram; dez tentativas de escalada cruzada foram
rejeitadas com SQLState `42501`; aplicação ficou insert-only, reader sem acesso
ao guard interno e operadores com `EXECUTE` apenas na própria função. Nenhum
capability role possuía tabelas ou funções, a versão da chave foi preservada
nos eventos e na execução de retenção, os papéis da execução foram removidos e
a branch foi excluída após a coleta.

## Prova concorrente repetível

O runner `scripts/workspace/Invoke-RuleAuditRetentionConcurrencyProof.ps1`
aplica as migrations operacionais em uma branch Neon efêmera e abre conexões
PostgreSQL independentes para provar os dois lados da corrida:

```powershell
$env:JAVA_HOME='D:\Developer\JAVA\openjdk-21_windows-x64_bin\jdk-21'
.\scripts\workspace\Invoke-RuleAuditRetentionConcurrencyProof.ps1 `
  -BranchHost '<endpoint-da-branch>.neon.tech' `
  -EphemeralBranch `
  -ConnectionStringFromClipboard
```

Antes da execução, copie o connection string administrativo exibido no modal
da branch `schema-only`. O runner o consome apenas em memória, limpa o clipboard,
confirma que o host corresponde a `-BranchHost`, rejeita o endpoint canônico,
não imprime credenciais e grava apenas evidência sanitizada em
`target/rule-audit-retention-concurrency-proof/concurrency-proof-evidence.json`.
No cenário `PLACE` vencedor, retenção deve pular a linha bloqueada e removê-la
somente após `RELEASE`. No cenário de expurgo vencedor, `PLACE` deve aguardar o
commit e falhar sem criar hold ou evento órfão. A branch efêmera deve ser
excluída depois da coleta.

Esta é uma prova técnica da serialização PostgreSQL, executada pelo owner para
instalar e exercitar a migration. Ela não substitui o gate corporativo de
provisionar identidades distintas e conceder `EXECUTE` separadamente ao
operador de legal hold e ao scheduler de retenção.

Evidência coletada em 2026-07-15 numa branch Neon `schema-only` efêmera: no
cenário `PLACE` vencedor, a retenção registrou zero exclusões durante o lock,
preservou a linha e manteve os dois eventos `PLACE`/`RELEASE`; no cenário de
expurgo vencedor, `PLACE` aguardou o commit, foi rejeitado e deixou zero hold e
zero evento órfão. O runner terminou com `ADR12_CONCURRENCY_PROOF_PASS` e a
branch foi excluída após a coleta.

## Limites da prova

A migration oferece a fronteira técnica e fail-closed. Canonicalização, key id
e rotação HMAC foram provados apenas com segredos fictícios. Prazo legal, owner
da política, secret manager e rotação reais, scheduler, dashboards, alertas, SIEM, backup,
restore e aprovação de compliance pertencem ao ambiente-alvo. Sem essas
evidências, o laboratório não autoriza shadow, preflight, autoridade Ergon ou
desligamento legado.
