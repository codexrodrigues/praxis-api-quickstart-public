# Migrations do datasource operacional da API

O Quickstart usa dois bancos com responsabilidades diferentes:

- `spring.datasource.*`: datasource operacional da API, onde vivem tabelas do dominio de exemplo como `funcionarios`, `eventos_folha`, `legacy_pay_codes`, `equipamentos`, `veiculos`, `ameacas` e views de analytics.
- `config.datasource.*`: datasource do Praxis Config Starter, usado por config store, AI registry, domain catalog, Project Knowledge, Domain Knowledge, RAG e outras superficies em `/api/praxis/config/**`.

No `application.properties`, o Flyway esta ligado ao `config.datasource.*`. Isso e intencional: ele governa o config/RAG store. As mudancas de schema do banco operacional da API ficam versionadas em [`db/operational-migrations`](../db/operational-migrations).

## Por que esta trilha existe

O cockpit e o metadata starter conseguem descobrir recursos, endpoints, actions, surfaces, filtros e capabilities pelo contrato publicado pelo host. Mas isso nao prova sozinho que o banco operacional publicado no ambiente esta alinhado ao contrato JPA do recurso.

Esse drift apareceu quando `human-resources.legacy-pay-codes` foi publicado corretamente no cockpit, mas a tabela `public.legacy_pay_codes` ainda nao existia no Neon operacional. O problema nao era do starter; era ausencia de uma trilha operacional versionada para o datasource da API.

## Como aplicar

Use uma credencial owner/admin ou uma credencial tecnica de migracao com permissao de DDL. Nao use o usuario runtime do Render se ele for limitado a DML/leitura.

```bash
javac scripts/JdbcSqlRunner.java
java -cp "scripts:$POSTGRES_JDBC_JAR" JdbcSqlRunner \
  db/operational-migrations/V20260702_001__legacy_pay_codes.sql \
  db/operational-migrations/V20260702_002__eventos_folha_status.sql \
  db/operational-migrations/V20260703_001__purchase_order_lifecycle.sql \
  db/operational-migrations/V20260703_002__assets_cockpit_demo_seed.sql \
  db/operational-migrations/V20260703_003__procurement_cockpit_lifecycle_seed.sql \
  db/operational-migrations/V20260711_001__resource_action_transition_and_versions.sql \
  db/operational-migrations/V20260711_002__resource_action_execution_idempotency.sql \
  db/operational-migrations/V20260713_001__scope_action_idempotency_by_resource_and_actor.sql \
  db/operational-migrations/V20260713_002__extraordinary_benefit_request_lifecycle.sql
```

Antes de executar, configure no shell as tres variaveis de ambiente do datasource operacional declaradas em `application.properties`: URL JDBC, usuario e senha do `spring.datasource.*`. `POSTGRES_JDBC_JAR` deve apontar para o driver PostgreSQL local. Em ambientes automatizados, prefira usar o classpath Maven ou a ferramenta oficial de migracao do provedor, mantendo a ordem dos arquivos.

## Como verificar drift antes do cockpit

Depois de aplicar migrations, rode o verificador com uma credencial que tenha acesso de leitura ao schema:

```bash
javac scripts/ApiOperationalSchemaDriftCheck.java
java -cp "scripts:$POSTGRES_JDBC_JAR" ApiOperationalSchemaDriftCheck
```

Antes de executar, configure no shell as tres variaveis de ambiente do datasource operacional declaradas em `application.properties`. Para o drift check, uma credencial somente leitura e suficiente.

O check falha com exit code diferente de zero quando um objeto operacional essencial esta ausente. Hoje ele valida:

- tabela `public.legacy_pay_codes`
- colunas de contrato de `public.legacy_pay_codes`
- coluna `public.eventos_folha.status`
- tabela `public.procurement_purchase_orders`
- colunas de ciclo de vida de `public.procurement_purchase_orders`
- colunas `version` de `public.funcionarios` e `public.eventos_folha`
- tabela e colunas essenciais de `public.praxis_resource_action_transition`
- tabela `public.praxis_resource_action_execution` para replay idempotente escopado por recurso e ator
- agregado `public.extraordinary_benefit_request` e ledger `public.extraordinary_benefit_grant_effect`

Para actions que aceitam `Idempotency-Key`, a chave identifica a requisição completa dentro do
escopo `resource + item/collection + action + ator`. Repetir a mesma chave com o mesmo comando
devolve o resultado persistido; reutilizá-la com payload diferente retorna conflito. A tabela de
execução não substitui a trilha por item em `praxis_resource_action_transition`.

Esse conjunto deve crescer sempre que o Quickstart publicar um novo recurso JPA cujo schema operacional nao esteja coberto por dump, migration ou teste de bootstrap.

## Regra para novos recursos do Quickstart

Ao criar ou promover um recurso JPA publico:

1. Confirme se a fonte operacional ja existe no dump ou no banco de referencia.
2. Se houver mudanca de schema/seed minimo, adicione migration em `db/operational-migrations`.
3. Seeds demonstrativas devem ser idempotentes e conservadoras: nao sobrescreva linhas por ID sem reconhecer que sao dados demo esperados, e prefira pular a insercao quando uma base corporativa ja usa aquele identificador para outro ativo.
4. Atualize o drift check quando a ausencia do objeto puder quebrar endpoint, capabilities ou workflow action no ambiente publicado.
5. Adicione teste focal H2 para provar o contrato HTTP/schema/actions do recurso.
6. Depois do deploy, valide o recurso publicado por HTTP real antes de tratar o cockpit como evidencia final.

O app runtime nao deve executar DDL automaticamente no datasource operacional. Essa separacao preserva o principio de menor privilegio e deixa claro quando o problema e contrato semantico, drift de banco ou permissao operacional.
