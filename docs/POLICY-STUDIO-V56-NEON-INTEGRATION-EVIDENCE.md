# Policy Studio V56 — evidência integrada no Neon

Data da execução: 2026-08-13.

Esta evidência registra o primeiro gate PostgreSQL/HTTP das asserções semânticas
de Test Run. Ela não é um certificado de release e não declara o lifecycle
maker-checker completo como aprovado.

## Topologia validada

- Quickstart local na porta oficial `8088`, perfil `dev`;
- datasource operacional Neon separado do datasource Config Neon;
- Flyway do host apontando para o datasource Config;
- schema Config `public` validado em leitura antes da migração;
- `V56__add_policy_test_run_semantic_assertions.sql` aplicada pelo Flyway e
  registrada com `success=true` após V55;
- host reiniciado sobre o mesmo datasource e validando 54 migrations, sem drift.

O Studio não criou banco, schema ou tabela próprios. Workspaces, cenários e Test
Runs permaneceram no control plane do Config; o datasource operacional continuou
separado para fatos, efeitos e observações host-owned.

## Fluxo HTTP executado

Foi criado um workspace isolado para `grant.amount-parameters` e um cenário
`ALLOW` com facts completos, nenhuma razão esperada e a intenção de efeito
esperada `REGISTER_EXTRAORDINARY_GRANT`. O sandbox host-owned retornou:

- candidato `ALLOW` e `candidateMatchesExpected=true`;
- output não exigido e `candidateOutputMatchesExpected=true`;
- reasons e efeitos semânticos coincidentes;
- intenção de efeito apenas como `PLANNED_NOT_EXECUTED`;
- snapshot ativo ausente, lane ativa `TECHNICAL_ERROR` e comparação
  `TECHNICAL_ERROR`.

O Config persistiu o Test Run, recalculou os matches contra o cenário persistido
e publicou somente `VIEW`, `UPDATE_DRAFT`, `MANAGE_SCENARIOS` e
`RECORD_TEST_RUN`. `SUBMIT` permaneceu ausente, com blocker
`TEST_RUN_NOT_PASSING`.

As negativas corporativas também foram exercitadas:

- submissão com o ETag corrente: `409`, porque a lane ativa não passou;
- atualização com ETag obsoleto: `412` e pedido explícito de reload;
- leitura anônima das capabilities do workspace: `403`.

Nenhuma aprovação, publicação, ativação ou execução de efeito operacional foi
disparada por essa prova.

## Conclusão e próximo gate

A V56 e o fail-closed de submissão estão provados no PostgreSQL real. O lifecycle
completo ainda está bloqueado por um pré-requisito legítimo: não existe head ativo
para o RuleSet `extraordinary-grant-eligibility` nesse escopo.

O próximo gate deve provisionar o baseline por
`scripts/workspace/Initialize-RuleLabQl07Snapshot.ps1`, com duas identidades
aprovadoras distintas e uma identidade publicadora. Depois disso, repetir
candidate × active, submissão, review por ator diferente, promoção e publication
readiness. Não substituir esse requisito por um snapshot sintético, por uma conta
`admin` superprivilegiada ou pela conversão de `TECHNICAL_ERROR` em sucesso.

## Continuação maker-checker

O gate seguinte foi executado no mesmo dia e provisionou um snapshot imutável
com sete fontes aprovadas, duas aprovações de composição de atores distintos e
publisher separado. As negativas confirmaram `403` para publisher tentando
aprovar e approver tentando publicar. Após restart, o host carregou o snapshot
persistido e o sandbox retornou `MATCH` nas lanes candidate × active, incluindo
decisão, reasons e efeitos planejados.

Durante a prova foram encontrados e corrigidos dois defeitos que só aparecem na
composição corporativa real:

- a regra HTTP genérica de `POST /workspaces/**` capturava review antes da regra
  de aprovador; a rota `POST /workspaces/{id}/reviews` agora exige
  `RULE_DEFINITION_APPROVER` explicitamente;
- `DomainRuleChangeWorkspaceService` usava o transaction manager implícito; em
  host com dois datasources, o lock de promoção era executado fora da transação
  Config. Todas as operações do serviço agora declaram
  `configTransactionManager`.

Com essas correções, um workspace novo ancorado na definição aprovada percorreu
`OPEN -> SUBMITTED -> APPROVED -> PROMOTED`, com Test Run semântico passando e
reviewer diferente do autor. A simulação estrutural da definição promovida
retornou `blocked_by_existing_coverage`, pois encontrou sete definições aprovadas
do mesmo tipo/recurso. A publicação não foi forçada: o próximo trabalho é definir
e provar a estratégia canônica de supersession/coverage, preservando o gate do
Config.

## Composição completa e publicação — gate de 2026-08-13

Depois da introdução do adapter host-owned de composição, a prova foi repetida
contra o mesmo Config Neon, no escopo server-owned `desenv/local`. O navegador
não enviou nem escolheu tenant ou ambiente. O candidato recomposto continha as
sete fontes obrigatórias e substituiu exatamente `grant.amount-parameters` pela
Definition promovida `b0918986-a64b-4046-8503-6fe7e7fef4f5`.

Resultados observados por HTTP real:

- preparação pelo publisher: `200`, versão candidata `2`, sete fontes e ações
  `PREPARE`/`PUBLISH`;
- publisher tentando aprovar: `403`;
- aprovações distintas por `policy-approver-a` e `policy-approver-b`: `200`;
- approver tentando publicar: `403`;
- publicação pelo publisher com `If-Match` do head: `201`;
- snapshot `7c004e02-2003-4570-b0da-30c05607e2b5`, revisão de ativação `2`,
  novo head ETag `c5dde313-528e-4341-8918-691e0b6ddffd`;
- repetição com digest adulterado: `409 STALE_RULESET_CANDIDATE`, sem mutação.

O boot também confirmou schema Config na versão `56`, com `54` migrations, e
datasources Neon distintos para Config e dados operacionais. Nenhuma tabela ou
schema exclusivo do Studio foi criado.

Dois pré-requisitos locais passaram a ser explícitos: o segredo de ETag deve ser
fornecido pelo deployment; e, quando o laboratório liga `corporate-mode`, o
tenant e o ambiente server-owned devem apontar para o escopo realmente
provisionado. O padrão corporativo permanece fail-closed e não aceita identidade
de escopo escolhida pelo browser.
