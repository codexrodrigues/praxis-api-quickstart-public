Database migrations (manual) — Eventos de Folha (status)

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
- A action `POST /api/human-resources/eventos-folha/actions/bulk-approve` agora exige essa coluna para validar `allowedStates=PENDENTE` e persistir a transição para `APROVADO`.
- Sem a coluna, o endpoint continua respondendo `200`, mas cada item retorna falha com a mensagem `Workflow requires public.eventos_folha.status column`; não há aprovação simulada.
- O host reavalia a existência da coluna a cada nova chamada enquanto ela estiver ausente; depois da migration aplicada, não é necessário reiniciar a aplicação para o workflow voltar a operar.

