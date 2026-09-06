# Patch opt-in: coerência de dependentes no dossiê

Este patch corrige somente a duplicidade fictícia de Morgan Stark vinculada a Tony Stark na base
pública de demonstração. O registro canônico de ID `1`, presente desde a fixture original, é
preservado. O registro de ID `68`, introduzido posteriormente durante uma ampliação em massa do
seed, é removido.

O patch não altera schema, contrato HTTP nem regra de negócio e, por isso, não pertence a
`db/operational-migrations`. Antes de escrever, o SQL confirma a identidade imutável do
funcionário e aceita apenas um destes estados:

- estado antigo conhecido, com os registros `1` e `68` exatamente como publicados;
- estado final esperado, com o registro `1` preservado e o registro `68` ausente.

Qualquer divergência aborta a transação com `PRAXIS_DEMO_GUARD`. A remoção usa simultaneamente ID,
nome, parentesco, data de nascimento e funcionário, sem deduplicar por nome ou por heurística.

## Aplicação explícita

Use uma credencial de migração somente depois de confirmar que a conexão aponta para a demo
pública:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://db.example:5432/public-demo?sslmode=require'
export SPRING_DATASOURCE_USERNAME='migration_owner'
export SPRING_DATASOURCE_PASSWORD='REPLACE_WITH_DEMO_PASSWORD'
export POSTGRES_JDBC_JAR='/path/to/postgresql-driver.jar'

javac scripts/JdbcSqlRunner.java
java -cp "scripts:$POSTGRES_JDBC_JAR" JdbcSqlRunner \
  db/demo-seeds/public-demo/employee-dependent-coherence/V20260812_001__employee_dependent_coherence.sql
```

Execute o comando uma segunda vez para confirmar idempotência. Depois, valide por HTTP
`GET /api/human-resources/funcionarios/1/dependents`: a resposta deve conter uma única Morgan
Stark, com ID `1`, parentesco `Filha` e data de nascimento `2015-04-01`.
