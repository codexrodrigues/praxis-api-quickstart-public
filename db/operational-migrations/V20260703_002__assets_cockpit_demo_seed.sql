with seed(id, nome, tipo, capacidade, proprietario_id, status) as (
    values
        (1, 'Quinjet', 'AEREO', 12, 1, 'OPERACIONAL'),
        (2, 'Batmovel', 'TERRESTRE', 2, 3, 'OPERACIONAL'),
        (3, 'Batwing em Revisao', 'AEREO', 2, 3, 'MANUTENCAO'),
        (4, 'Helicarrier-01', 'AEREO', 300, 37, 'OPERACIONAL'),
        (5, 'Submersivel Atlante Bloqueado', 'MARITIMO', 6, 34, 'INOPERANTE'),
        (6, 'Nave Guardioes Milano', 'ESPACIAL', 8, 45, 'OPERACIONAL'),
        (7, 'Rover de Campo Sokovia', 'TERRESTRE', 4, 48, 'MANUTENCAO'),
        (8, 'Interceptor Wakanda', 'AEREO', 2, 28, 'OPERACIONAL')
)
update public.veiculos v
set nome = seed.nome,
    tipo = seed.tipo,
    capacidade = seed.capacidade,
    proprietario_id = seed.proprietario_id,
    status = seed.status
from seed
where v.id = seed.id
  and v.nome in ('Quinjet', 'Batmovel', 'Batmóvel', 'Batwing', 'Batwing em Revisao', 'Helicarrier-01');

with seed(id, nome, tipo, capacidade, proprietario_id, status) as (
    values
        (1, 'Quinjet', 'AEREO', 12, 1, 'OPERACIONAL'),
        (2, 'Batmovel', 'TERRESTRE', 2, 3, 'OPERACIONAL'),
        (3, 'Batwing em Revisao', 'AEREO', 2, 3, 'MANUTENCAO'),
        (4, 'Helicarrier-01', 'AEREO', 300, 37, 'OPERACIONAL'),
        (5, 'Submersivel Atlante Bloqueado', 'MARITIMO', 6, 34, 'INOPERANTE'),
        (6, 'Nave Guardioes Milano', 'ESPACIAL', 8, 45, 'OPERACIONAL'),
        (7, 'Rover de Campo Sokovia', 'TERRESTRE', 4, 48, 'MANUTENCAO'),
        (8, 'Interceptor Wakanda', 'AEREO', 2, 28, 'OPERACIONAL')
)
insert into public.veiculos (id, nome, tipo, capacidade, proprietario_id, status)
select seed.id, seed.nome, seed.tipo, seed.capacidade, seed.proprietario_id, seed.status
from seed
where not exists (
    select 1
    from public.veiculos v
    where v.id = seed.id
       or v.nome = seed.nome
);

with seed(id, veiculo_id, missao_id, piloto_id, partida, chegada, observacao) as (
    values
        (1, 1, 1, 47, timestamp with time zone '2025-10-18 10:41:43.995048+00', null, 'Deslocamento de reforcos SHIELD'),
        (2, 4, 32, 37, timestamp with time zone '2026-04-05 09:00:00+00', null, 'Comando aereo de suporte ao Escudo de Atlantis'),
        (3, 6, 33, 45, timestamp with time zone '2026-04-12 06:30:00+00', null, 'Escolta diplomatica Kree-Skrull'),
        (4, 7, 35, 48, timestamp with time zone '2026-05-02 10:00:00+00', timestamp with time zone '2026-05-03 18:00:00+00', 'Rover recolhido para manutencao apos auditoria remota'),
        (5, 8, 34, 28, timestamp with time zone '2026-01-10 12:00:00+00', timestamp with time zone '2026-01-21 16:00:00+00', 'Transporte de cristais para custodia controlada')
)
update public.veiculo_missao_usos vmu
set veiculo_id = seed.veiculo_id,
    missao_id = seed.missao_id,
    piloto_id = seed.piloto_id,
    partida = seed.partida,
    chegada = seed.chegada,
    observacao = seed.observacao
from seed
where vmu.id = seed.id
  and vmu.observacao in (
      'Deslocamento de reforcos SHIELD',
      'Deslocamento de reforços SHIELD',
      'Comando aereo de suporte ao Escudo de Atlantis',
      'Escolta diplomatica Kree-Skrull',
      'Rover recolhido para manutencao apos auditoria remota',
      'Transporte de cristais para custodia controlada'
  );

with seed(id, veiculo_id, missao_id, piloto_id, partida, chegada, observacao) as (
    values
        (1, 1, 1, 47, timestamp with time zone '2025-10-18 10:41:43.995048+00', null, 'Deslocamento de reforcos SHIELD'),
        (2, 4, 32, 37, timestamp with time zone '2026-04-05 09:00:00+00', null, 'Comando aereo de suporte ao Escudo de Atlantis'),
        (3, 6, 33, 45, timestamp with time zone '2026-04-12 06:30:00+00', null, 'Escolta diplomatica Kree-Skrull'),
        (4, 7, 35, 48, timestamp with time zone '2026-05-02 10:00:00+00', timestamp with time zone '2026-05-03 18:00:00+00', 'Rover recolhido para manutencao apos auditoria remota'),
        (5, 8, 34, 28, timestamp with time zone '2026-01-10 12:00:00+00', timestamp with time zone '2026-01-21 16:00:00+00', 'Transporte de cristais para custodia controlada')
)
insert into public.veiculo_missao_usos (id, veiculo_id, missao_id, piloto_id, partida, chegada, observacao)
select seed.id, seed.veiculo_id, seed.missao_id, seed.piloto_id, seed.partida, seed.chegada, seed.observacao
from seed
where exists (select 1 from public.veiculos v where v.id = seed.veiculo_id)
  and exists (select 1 from public.missoes m where m.id = seed.missao_id)
  and exists (select 1 from public.funcionarios f where f.id = seed.piloto_id)
  and not exists (
      select 1
      from public.veiculo_missao_usos vmu
      where vmu.id = seed.id
         or (vmu.veiculo_id = seed.veiculo_id and vmu.missao_id = seed.missao_id)
  );

select nextval('public.veiculos_id_seq')
from generate_series(
    1,
    greatest(0, (select coalesce(max(id), 1) from public.veiculos) - (select last_value from public.veiculos_id_seq)::integer)
);

select nextval('public.veiculo_missao_usos_id_seq')
from generate_series(
    1,
    greatest(0, (select coalesce(max(id), 1) from public.veiculo_missao_usos) - (select last_value from public.veiculo_missao_usos_id_seq)::integer)
);
