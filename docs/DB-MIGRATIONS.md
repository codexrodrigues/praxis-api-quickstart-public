Database migrations (manual) - Eventos de Folha (status)

> Nota canonica: a trilha versionada do datasource operacional da API agora fica em
> [`db/operational-migrations`](../db/operational-migrations). O processo completo esta em
> [`docs/OPERATIONAL-DATASOURCE-MIGRATIONS.md`](OPERATIONAL-DATASOURCE-MIGRATIONS.md).

Para habilitar a persistência da action tipada `bulk-approve` em `eventos_folha`, adicione a coluna `status` (enum textual) com default `PENDENTE`:

PostgreSQL:
```
ALTER TABLE public.eventos_folha
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

-- Opcional: índice para filtros por status
CREATE INDEX IF NOT EXISTS idx_eventos_folha_status ON public.eventos_folha(status);
```

Observações:
- Ambientes de DEV deste projeto usam `spring.jpa.hibernate.ddl-auto=none`; portanto, a alteração não é aplicada automaticamente.
- Em PROD, use ferramenta de migração (Flyway/Liquibase) ou scripts manuais versionados.
- A action `POST /api/human-resources/eventos-folha/actions/bulk-approve` usa esse estado como fonte de verdade JPA para validar `allowedStates=PENDENTE` e persistir a transição para `APROVADO`.
- A coluna é obrigatória: o host não mantém fallback para banco sem a migration. Execute a trilha operacional antes de publicar o workflow.
