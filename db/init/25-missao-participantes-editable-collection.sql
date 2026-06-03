-- Prepara missao_participantes para o exemplo real de Editable Collection / Array Field.
-- Idempotente para dumps existentes importados por 20-import-dump.sql.

ALTER TABLE public.missao_participantes
    ADD COLUMN IF NOT EXISTS ordem integer,
    ADD COLUMN IF NOT EXISTS principal boolean;

WITH ordered AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY missao_id
               ORDER BY
                   CASE WHEN papel = 'LIDER' THEN 0 ELSE 1 END,
                   id
           ) - 1 AS nova_ordem
      FROM public.missao_participantes
)
UPDATE public.missao_participantes mp
   SET ordem = ordered.nova_ordem
  FROM ordered
 WHERE mp.id = ordered.id
   AND mp.ordem IS NULL;

UPDATE public.missao_participantes mp
   SET principal = (mp.papel = 'LIDER')
 WHERE mp.principal IS NULL;

WITH first_participant AS (
    SELECT DISTINCT ON (missao_id) id
      FROM public.missao_participantes
     ORDER BY missao_id, ordem, id
)
UPDATE public.missao_participantes mp
   SET principal = true
  FROM first_participant fp
 WHERE mp.id = fp.id
   AND NOT EXISTS (
       SELECT 1
         FROM public.missao_participantes existing
        WHERE existing.missao_id = mp.missao_id
          AND existing.principal IS TRUE
   );

ALTER TABLE public.missao_participantes
    ALTER COLUMN ordem SET DEFAULT 0,
    ALTER COLUMN ordem SET NOT NULL,
    ALTER COLUMN principal SET DEFAULT false,
    ALTER COLUMN principal SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'missao_participantes_ordem_nonnegative_check'
           AND conrelid = 'public.missao_participantes'::regclass
    ) THEN
        ALTER TABLE public.missao_participantes
            ADD CONSTRAINT missao_participantes_ordem_nonnegative_check CHECK (ordem >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_miss_part_missao_ordem
    ON public.missao_participantes (missao_id, ordem);

CREATE UNIQUE INDEX IF NOT EXISTS ux_miss_part_principal_por_missao
    ON public.missao_participantes (missao_id)
    WHERE principal IS TRUE;
