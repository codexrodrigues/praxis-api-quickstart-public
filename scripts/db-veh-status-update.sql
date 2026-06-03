-- Script manual para atualizar status de veículos para OPERACIONAL
-- Uso (Docker):
--   docker exec -i praxis-postgres psql -U postgres -d neondb -f - < scripts/db-veh-status-update.sql

BEGIN;

-- Concluir manutenção do Batmóvel (2) e Batpod (6)
UPDATE public.veiculos SET status = 'OPERACIONAL' WHERE id IN (2, 6);

-- Reativar Submarino Atlante (9)
UPDATE public.veiculos SET status = 'OPERACIONAL' WHERE id = 9;

-- Verificação rápida
SELECT id, nome, status FROM public.veiculos WHERE id IN (2, 6, 9) ORDER BY id;

COMMIT;

