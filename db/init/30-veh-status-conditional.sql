-- Atualiza status de veículos após datas específicas (aplicado no boot inicial)
-- Evita DO $$; usa SQL puro com condição baseada em now()

-- Batmóvel (2) e Batpod (6) saem de manutenção a partir de 2025-11-05
UPDATE public.veiculos
   SET status = 'OPERACIONAL'
 WHERE id IN (2, 6)
   AND now() >= TIMESTAMPTZ '2025-11-05 00:00:00+00'
   AND status <> 'OPERACIONAL';

-- Submarino Atlante (9) volta a operacional a partir de 2025-11-09
UPDATE public.veiculos
   SET status = 'OPERACIONAL'
 WHERE id = 9
   AND now() >= TIMESTAMPTZ '2025-11-09 00:00:00+00'
   AND status <> 'OPERACIONAL';

