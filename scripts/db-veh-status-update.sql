-- Script manual para atualizar status de veiculos para OPERACIONAL
-- Uso (Docker):
--   docker exec -i praxis-postgres psql -U postgres -d praxis_demo -f - < scripts/db-veh-status-update.sql

BEGIN;

-- Concluir manutencao do Batmovel (2) e Batpod (6)
UPDATE public.veiculos SET status = 'OPERACIONAL' WHERE id IN (2, 6);

-- Reativar Submarino Atlante (9)
UPDATE public.veiculos SET status = 'OPERACIONAL' WHERE id = 9;

-- Verificacao rapida
SELECT id, nome, status FROM public.veiculos WHERE id IN (2, 6, 9) ORDER BY id;

COMMIT;
