# API operational datasource migrations

Esta pasta versiona mudancas de schema e seed minimo do datasource operacional da API, isto e, o banco usado por `spring.datasource.*`.

Ela nao substitui o Flyway configurado em `src/main/resources/application.properties`. No Quickstart,
`spring.flyway.*` aponta para `config.datasource.*`, porque o starter de configuracao governa o
config/RAG store. As migrations desta pasta devem ser aplicadas separadamente, por um usuario
owner/admin ou por um usuario tecnico de migracao com permissao de DDL no schema alvo.

O deploy oficial empacota explicitamente apenas migrations operacionais aprovadas para a lane
pre-deploy em `db/operational-runtime-migrations` dentro do JAR. A fonte continua sendo esta pasta;
nao existe uma segunda copia do SQL. A lane usa `praxis_api_schema_history`, nunca a history table do
Config Starter, e precisa terminar antes da inicializacao do runtime com `ddl-auto=validate`.

Todas as migrations `V*.sql` desta pasta devem ter versao Flyway unica. A fixture Render restaura o
dump publico canônico em schema vazio e registra baseline anterior a esta trilha, permitindo que a
mesma sequencia inteira reconcilie o snapshot ao contrato atual. O modo corporativo de schema
existente conserva seu baseline de adocao e nunca importa o dump demo.

Ordem atual:

1. `V20260702_001__legacy_pay_codes.sql`
2. `V20260702_002__eventos_folha_status.sql`
3. `V20260703_001__purchase_order_lifecycle.sql`
4. `V20260703_002__assets_cockpit_demo_seed.sql`
5. `V20260703_003__procurement_cockpit_lifecycle_seed.sql`
6. `V20260711_001__resource_action_transition_and_versions.sql`
7. `V20260711_002__resource_action_execution_idempotency.sql`
8. `V20260713_001__scope_action_idempotency_by_resource_and_actor.sql`
9. `V20260713_002__extraordinary_benefit_request_lifecycle.sql`
10. `V20260714_001__historical_department_assignments.sql`
11. `V20260714_002__absence_analytics_view.sql`
12. `V20260714_003__extraordinary_benefit_statement_outbox.sql` — outbox QL-08, instante da última
    falha e auditoria append-only de replay governado.

13. `V20260715_001__extraordinary_benefit_transformation_audit.sql` — evidência QL-08 redigida e
    append-only das transformações autorizadas pelo host, sem valores ou fatos serializados.
14. `V20260715_002__rule_audit_privacy_retention.sql` — fronteira P2F-ADR-12 para imutabilidade,
    legal hold e expurgo limitado das trilhas de replay e transformação, com registro append-only
    de cada decisão de retenção e sem ator, ticket ou justificativa em texto aberto.
15. `V20260715_003__extraordinary_benefit_transformation_audit_digest_varchar.sql` — corrige
    instalações ADR-11 já aplicadas, alinhando os digestos redigidos ao contrato JPA `varchar(64)`
    sem alterar o valor da evidência.
16. `V20260715_004__rule_audit_authorization_key_rotation.sql` — registra o identificador opaco e
    versionado da chave HMAC em legal hold e retenção, migra evidência anterior como
    `LEGACY-UNVERSIONED` e remove as assinaturas sem key id para impedir bypass de provenance.
17. `V20260715_005__absence_analytics_unique_days_policy.sql` — reconcilia o piloto G2 para
   dias únicos por colaborador, lotação efetiva e competência, com a política `2026-07-15`.
18. `V20260715_005_001__extraordinary_benefit_local_effect_semantics.sql` — distingue o registro
   local governado de um efeito externo; o sufixo resolve a ordenação Flyway sem apagar o corte 005.
19. `V20260715_006__extraordinary_benefit_authoritative_facts.sql` — instala políticas e janelas
   tipadas do piloto de benefício extraordinário.
20. `V20260716_001__authoritative_rule_fact_source.sql` — instala a fonte operacional tipada,
   temporal e escopada usada pelo adapter FND-06 de facts server-side; a migration revoga `PUBLIC`
   e o deploy deve conceder somente `SELECT` ao papel runtime concreto do ambiente.
21. `V20260716_002__authoritative_rule_fact_demo_seed.sql` — adiciona uma fixture fictícia e
   sanitizada para provar aquisição e proveniência. Não representa baseline Ergon nem autoriza shadow.
22. `V20260716_003__payroll_analytics_effective_department.sql` — atribui cada fato de folha ao
   departamento efetivo no primeiro dia da competência, usando intervalo temporal semiaberto e sem
   fallback para o departamento atual. O drift check falha quando houver folha sem lotação vigente.
23. `V20260716_004__authoritative_fact_apply_revalidation.sql` - acrescenta provenance inicial ao
   agregado e evidencia redigida da revalidacao executada na mesma transacao antes do efeito;
   registros sem provenance permanecem inelegiveis para `apply`.
24. `V20260813_001__rule_execution_observation_outbox.sql` — outbox redigido e idempotente de
   observações do runtime, incluindo índices de dispatch e lease expirada.
25. `V20260814_001__extraordinary_benefit_reevaluation_audit.sql` — amplia a unicidade do ledger
   para facts autoritativos distintos. Em schemas existentes, aceita a constraint alvo já
   materializada somente quando tipo e ordem das colunas forem equivalentes; uma definição
   homônima incompatível falha de forma explícita, sem ser sobrescrita.
26. `V20260826_001__supplier_procurement_funnel_view.sql` — publica seis etapas cumulativas por
   empresa a partir de fornecedores, homologacao, disponibilidade operacional, contratos e pedidos
   persistidos. A view nao copia a policy dinamica de selecao do Config Starter.
27. `V20260905_001__acordos_regulatorios_resource_version.sql` — acrescenta a versao persistida
   usada por `If-Match` nas actions ITEM governadas de acordos regulatorios.

Depois de aplicar a trilha, execute o drift check:

```bash
javac scripts/ApiOperationalSchemaDriftCheck.java
java -cp "scripts:$POSTGRES_JDBC_JAR" ApiOperationalSchemaDriftCheck
```

Use as variaveis de ambiente do datasource operacional declaradas em `application.properties`, apontando para o banco da API. Para aplicar migrations, use credencial de migracao; para o drift check, uma credencial somente leitura e suficiente.

Quando a credencial de migração não for a mesma identidade usada pela aplicação, confirme após a
aplicação que o papel de runtime recebeu somente `INSERT` na tabela
`extraordinary_benefit_transformation_audit` e que o papel de auditoria recebeu `SELECT`. `UPDATE`
e `DELETE` diretos permanecem bloqueados pelos triggers append-only, inclusive se forem concedidos
por engano. A mesma política vale para `extraordinary_benefit_statement_replay_audit`.

O deployment deve usar identidades distintas e conceder explicitamente:

- runtime: somente `INSERT` nos dois ledgers, sem acesso às tabelas de controle;
- auditoria: somente `SELECT` nos ledgers e nos registros de retenção/legal hold necessários;
- compliance: apenas `EXECUTE` em `record_rule_audit_legal_hold(...)`;
- retenção: apenas `EXECUTE` em `purge_rule_audit(...)`.

As funções nascem sem `EXECUTE` para `PUBLIC`. O parâmetro
`authorization_reference_digest` deve ser um HMAC-SHA-256 uppercase produzido fora do banco com
segredo corporativo; `authorization_key_id` deve ser um alias opaco e versionado, nunca o path do
secret manager. SHA-256 simples de usuário, chamado ou ticket de baixa entropia não é anonimização.
Após rotação, novos atos usam somente a chave ativa; a versão anterior permanece disponível pelo
prazo governado necessário à verificação de evidência histórica. O scheduler deve usar UUID novo
por execução, cutoff derivado da política aprovada e
lotes limitados. Registros sob legal hold não são removidos. O owner de migração continua sendo uma
identidade administrativa de emergência e não pode ser reutilizado pela aplicação ou pelo job.
