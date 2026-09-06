# Seed opt-in: analytics de afastamentos

Este diretório contém dados exclusivamente da base pública de demonstração. Ele não faz parte de
`db/operational-migrations` e nunca deve ser acrescentado a uma execução padrão de migrations.

O arquivo `V20260715_006__absence_analytics_demo_seed.sql` é idempotente e começa verificando:

- o sentinel `public.praxis_demo_dataset_guard` com a chave `praxis-public-demo`;
- o fingerprint `praxis-public-demo-2026-07-15`;
- o roster público esperado de colaboradores `1..50` e departamentos `1..27`;
- identidades imutáveis de colaboradores do dump (nome, CPF e e-mail), além dos IDs.

Se qualquer precondição falhar, o primeiro comando gera `PRAXIS_DEMO_GUARD` e não inicia a carga.
`JdbcSqlRunner` envia o arquivo integral ao PostgreSQL e executa a invocação em uma única transação.
O bloco `DO $$` do guard não é fragmentado localmente. Uma falha gera exit code diferente de zero e
rollback, portanto não há escrita de lotações ou afastamentos em uma base ambígua ou corporativa.

## Aplicação explícita na demo pública

Depois de restaurar `db/dump/public-demo-seed.sql` e aplicar as migrations operacionais de schema,
execute somente quando a conexão apontar para a base pública de demonstração:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://db.example:5432/public-demo?sslmode=require'
export SPRING_DATASOURCE_USERNAME='migration_owner'
export SPRING_DATASOURCE_PASSWORD='REPLACE_WITH_DEMO_PASSWORD'
export POSTGRES_JDBC_JAR='/path/to/postgresql-driver.jar'

javac scripts/JdbcSqlRunner.java
java -cp "scripts:$POSTGRES_JDBC_JAR" JdbcSqlRunner \
  db/demo-seeds/public-demo/absence-analytics/V20260715_006__absence_analytics_demo_seed.sql
```

No PowerShell/Windows:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://db.example:5432/public-demo?sslmode=require'
$env:SPRING_DATASOURCE_USERNAME = 'migration_owner'
$env:SPRING_DATASOURCE_PASSWORD = 'REPLACE_WITH_DEMO_PASSWORD'
$env:POSTGRES_JDBC_JAR = 'C:\tools\postgresql-driver.jar'

javac scripts\JdbcSqlRunner.java
java -cp "scripts;$env:POSTGRES_JDBC_JAR" JdbcSqlRunner `
  db\demo-seeds\public-demo\absence-analytics\V20260715_006__absence_analytics_demo_seed.sql
```

Sucesso registra `Executed SQL file ...` e retorna `0`. Falha do fingerprint ou roster registra
`PRAXIS_DEMO_GUARD`, retorna código não zero e deixa as contagens de lotações e afastamentos
inalteradas. Depois de uma execução válida, rode novamente o mesmo comando e confirme que as
contagens permanecem estáveis; essa é a prova operacional de idempotência.

O seed continua fora do comando padrão de migrations estruturais. Não acrescente este arquivo à
lista de `docs/OPERATIONAL-DATASOURCE-MIGRATIONS.md`.

Uma instância Neon que recebeu a versão anterior do seed manualmente não deve receber rollback
automático. Confirme o dataset, reconcilie lotações e eventos com o responsável e registre o
fingerprint apenas se a instância for de fato a demo pública; em qualquer outro caso, carregue a
história corporativa pelo processo auditável do domínio.
