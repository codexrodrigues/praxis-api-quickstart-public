# Patch opt-in: coerência do endereço no dossiê

Este patch corrige somente a ficha fictícia de Pepper Potts na base pública de demonstração. Ele
não altera schema, contrato HTTP ou regra de negócio e, por isso, não pertence a
`db/operational-migrations`.

Antes de escrever, o SQL confirma o fingerprint `praxis-public-demo-2026-07-15`, a identidade
imutável da funcionária e a relação entre o endereço e o funcionário. O patch aceita somente o
estado antigo conhecido ou o estado final esperado; qualquer divergência aborta a transação com
`PRAXIS_DEMO_GUARD`.

Bancos públicos restaurados antes de o fingerprint entrar no dump devem executar primeiro
`V20260812_000__public_demo_dataset_guard_bootstrap.sql`. Esse bootstrap não confia apenas em
contagens: ele confirma o roster `1..50`, os departamentos e endereços canônicos, quatro
identidades fictícias imutáveis e o estado conhecido do endereço. Registros adicionais criados
durante a homologação são preservados.

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
  db/demo-seeds/public-demo/employee-address-coherence/V20260812_000__public_demo_dataset_guard_bootstrap.sql
java -cp "scripts:$POSTGRES_JDBC_JAR" JdbcSqlRunner \
  db/demo-seeds/public-demo/employee-address-coherence/V20260812_001__employee_address_coherence.sql
```

Execute os dois comandos uma segunda vez para confirmar idempotência. Depois, valide por HTTP a
surface de endereço do funcionário `2`; ela deve retornar `Avenida Park`, `10880`, `Apartamento
42`, `Midtown`, `Nova York`, `NY` e `10017-000`.
