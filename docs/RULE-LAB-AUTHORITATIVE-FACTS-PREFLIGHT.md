# Rule Lab — preflight DB-backed com fatos autoritativos

## Resultado

O Quickstart publica `POST /api/human-resources/extraordinary-benefit-requests/actions/evaluate-authoritative`
como fronteira read-only entre intenção do usuário e fatos corporativos. O caller informa somente
colaborador, referência, motivo, data do evento, valor, data pretendida e fuso. Situação funcional,
afastamento, duplicidade, política vigente, calendário e orçamento são resolvidos no datasource do
host e congelados antes da avaliação determinística.

Essa action não substitui os endpoints sintéticos do laboratório: `evaluate`, `evaluate-batch` e
`shadow-compare` continuam simulation-only e bloqueados em `prod`. Também não cria solicitação,
reserva orçamento, avança lifecycle ou executa efeito externo.

## Integridade do contrato

- a leitura usa uma transação `REPEATABLE_READ`;
- ausência de colaborador retorna `404`;
- política ausente, calendário vazio ou mais de uma política vigente para o mesmo motivo/data
  retorna `412` (fail-closed);
- a migration impede sobreposição de vigência por motivo no banco;
- o digest SHA-256 inclui todos os fatos que influenciam a decisão, sem publicar seus valores;
- a proveniência expõe somente nomes lógicos allowlisted, nunca SQL, tabelas físicas ou registros;
- a avaliação e a resolução dos fatos usam o mesmo instante UTC congelado.

## Preparação operacional

Aplicar com `MIGRATION_DATASOURCE_*` de uma identidade owner/migration e
`OPERATIONAL_RUNTIME_ROLE` apontando para o papel da API; não promova o usuário runtime a owner:

```text
db/operational-migrations/V20260715_006__extraordinary_benefit_authoritative_facts.sql
```

Depois, executar `scripts/ApiOperationalSchemaDriftCheck.java`. O runtime não cria essas tabelas
automaticamente e recebe apenas `SELECT` nas fontes do preflight. Os seeds são fictícios e servem
somente ao laboratório; um host corporativo deve
substituí-los por projeções governadas de política, orçamento, calendário e histórico de concessões.

### Evidência do ambiente de referência — 2026-07-15

- migration `V20260715_006` aplicada no Neon operacional de referência do Quickstart;
- migrations anteriormente divergentes `V20260714_002` e `V20260715_004` reconciliadas no mesmo
  ambiente;
- `ApiOperationalSchemaDriftCheck --scope=authoritative-facts` passou com a identidade runtime,
  comprovando existência, colunas e privilégio `SELECT` nas três fontes;
- `ApiOperationalSchemaDriftCheck` global passou com a mesma identidade runtime.

Essa evidência prova schema e acesso mínimo no banco real.

### Evidência HTTP end-to-end — 2026-07-15

- sessão administrativa autenticada e colaborador selecionável resolvido pela option source real;
- `POST .../actions/evaluate-authoritative` retornou HTTP `200` e outcome `ALLOW`;
- snapshot governado `extraordinary-grant-eligibility` versão `2` efetivamente avaliado;
- `persisted=false` e `localEffectRecorded=false`;
- digest de fatos com 64 caracteres e quatro fontes lógicas sanitizadas;
- contagens de requests, effects, executions e transitions idênticas antes e depois da chamada.

O snapshot v2 foi publicado para superseder, sem apagar, o snapshot beta pre-manifest preservado no
Config Starter. O payload legado permaneceu fail-closed e não foi executado.

## UX e operação corporativa

No cockpit, a action deve ser apresentada como **Pré-avaliar com fatos corporativos**, deixando
visível que o resultado é read-only e não equivale a aprovação. A evidência segura permite suporte e
auditoria técnica sem revelar dados pessoais ou saldo orçamentário.

Antes de habilitar autoridade de escrita ainda são obrigatórios:

1. revalidar os fatos dentro da transação do comando e reservar orçamento de forma concorrente;
2. vincular identidade, tenant e escopo organizacional ao FactProvider;
3. definir ownership, SLO, observabilidade e retenção da fonte de fatos;
4. validar HTTP real no ambiente publicado e executar shadow contra a baseline aprovada;
5. integrar o efeito por outbox/inbox idempotente, sem transformar `ALLOW` em pagamento direto.

Portanto, este passo remove o risco de o cliente forjar fatos no preflight, mas deliberadamente não
promove o Quickstart a sistema de decisão financeira ou de folha.
