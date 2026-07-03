begin;

/*
 * Demo analytics seed for active employees.
 *
 * Dashboard targets:
 * - payroll: salary bands, deductions, additions and workflow status
 * - reputation: public vs governmental score contrast
 * - missions: status, priority, threat and event volume
 * - risk: active threats by class, level, status and reward
 * - compliance: operation licenses by level and expiration window
 * - assets: equipment by type, status and custody outcome
 * - profile: active employee coverage across skills, missions, media and bases
 *
 * This script intentionally creates a small number of controlled outliers.
 * It should stay idempotent and preserve the core operational coverage.
 */

update public.reputacoes
set score_publico = 58,
    score_governamental = 92,
    atualizado_em = timestamp with time zone '2026-05-26 10:00:00-03'
where funcionario_id = 37;

update public.reputacoes
set score_publico = 93,
    score_governamental = 64,
    atualizado_em = timestamp with time zone '2026-05-26 10:00:00-03'
where funcionario_id = 7;

update public.reputacoes
set score_publico = 49,
    score_governamental = 41,
    atualizado_em = timestamp with time zone '2026-05-26 10:00:00-03'
where funcionario_id in (6, 14);

update public.licencas_operacao
set valido_ate = date '2026-04-30'
where funcionario_id in (5, 10, 37)
  and valido_ate > date '2026-04-30';

update public.licencas_operacao
set valido_ate = date '2026-06-15'
where funcionario_id in (20, 28, 43)
  and valido_ate > date '2026-06-15';

with seed(nome, tipo, resistencia, proprietario_id, status, aloc_status, inicio, fim) as (
    values
        ('Armadura Mark XLII em Manutencao', 'ARMADURA', 9, 1, 'MANUTENCAO', 'DEVOLVIDO', timestamp with time zone '2026-03-12 09:00:00-03', timestamp with time zone '2026-05-20 18:00:00-03'),
        ('Drone de Reconhecimento Danificado', 'GADGET', 5, 5, 'QUEBRADO', 'DANIFICADO', timestamp with time zone '2026-04-02 09:00:00-03', timestamp with time zone '2026-04-18 15:00:00-03'),
        ('Modulo de Comunicacao Extraviado', 'GADGET', 4, 37, 'PERDIDO', 'PERDIDO', timestamp with time zone '2026-02-08 09:00:00-03', timestamp with time zone '2026-04-10 12:00:00-03'),
        ('Kit de Campo Reserva Wakanda', 'ARTEFATO', 8, 28, 'ESTOQUE', 'DEVOLVIDO', timestamp with time zone '2026-01-20 09:00:00-03', timestamp with time zone '2026-02-12 12:00:00-03'),
        ('Sensor Tatico Ravencroft', 'GADGET', 6, 15, 'MANUTENCAO', 'DANIFICADO', timestamp with time zone '2026-02-01 09:00:00-03', timestamp with time zone '2026-02-15 12:00:00-03'),
        ('Artefato Psionico Lacrado', 'ARTEFATO', 10, 43, 'ESTOQUE', 'DEVOLVIDO', timestamp with time zone '2026-03-03 09:00:00-03', timestamp with time zone '2026-03-22 12:00:00-03')
),
to_insert as (
    select s.*
    from seed s
    where not exists (
        select 1
        from public.equipamentos e
        where e.nome = s.nome
          and e.proprietario_id = s.proprietario_id
    )
)
insert into public.equipamentos (nome, tipo, resistencia, proprietario_id, status)
select nome, tipo, resistencia, proprietario_id, status
from to_insert;

with seed(nome, tipo, resistencia, proprietario_id, status, aloc_status, inicio, fim) as (
    values
        ('Armadura Mark XLII em Manutencao', 'ARMADURA', 9, 1, 'MANUTENCAO', 'DEVOLVIDO', timestamp with time zone '2026-03-12 09:00:00-03', timestamp with time zone '2026-05-20 18:00:00-03'),
        ('Drone de Reconhecimento Danificado', 'GADGET', 5, 5, 'QUEBRADO', 'DANIFICADO', timestamp with time zone '2026-04-02 09:00:00-03', timestamp with time zone '2026-04-18 15:00:00-03'),
        ('Modulo de Comunicacao Extraviado', 'GADGET', 4, 37, 'PERDIDO', 'PERDIDO', timestamp with time zone '2026-02-08 09:00:00-03', timestamp with time zone '2026-04-10 12:00:00-03'),
        ('Kit de Campo Reserva Wakanda', 'ARTEFATO', 8, 28, 'ESTOQUE', 'DEVOLVIDO', timestamp with time zone '2026-01-20 09:00:00-03', timestamp with time zone '2026-02-12 12:00:00-03'),
        ('Sensor Tatico Ravencroft', 'GADGET', 6, 15, 'MANUTENCAO', 'DANIFICADO', timestamp with time zone '2026-02-01 09:00:00-03', timestamp with time zone '2026-02-15 12:00:00-03'),
        ('Artefato Psionico Lacrado', 'ARTEFATO', 10, 43, 'ESTOQUE', 'DEVOLVIDO', timestamp with time zone '2026-03-03 09:00:00-03', timestamp with time zone '2026-03-22 12:00:00-03')
),
equipment_for_alloc as (
    select
        e.id,
        e.nome,
        e.proprietario_id,
        s.aloc_status,
        s.inicio,
        s.fim
    from public.equipamentos e
    join seed s
      on s.nome = e.nome
     and s.proprietario_id = e.proprietario_id
),
alloc_to_insert as (
    select
        efa.id as equipamento_id,
        efa.proprietario_id as funcionario_id,
        efa.inicio,
        efa.fim,
        efa.aloc_status as status
    from equipment_for_alloc efa
    where not exists (
        select 1
        from public.equipamento_alocacoes ea
        where ea.equipamento_id = efa.id
          and ea.funcionario_id = efa.proprietario_id
          and ea.inicio = efa.inicio
    )
)
insert into public.equipamento_alocacoes (equipamento_id, funcionario_id, inicio, fim, status)
select equipamento_id, funcionario_id, inicio, fim, status
from alloc_to_insert;

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

update public.ameacas
set status = 'CONFRONTO',
    recompensa = greatest(coalesce(recompensa, 0), 1250000.00)
where id in (1, 7, 13);

update public.ameacas
set status = 'LIVRE',
    recompensa = greatest(coalesce(recompensa, 0), 650000.00)
where id in (2, 9, 16);

commit;

with active_coverage as (
    select
        f.id,
        count(distinct ea.id) filter (where ea.status = 'ATIVO' and ea.fim is null) as equipamentos_ativos,
        count(distinct lo.id) as licencas,
        count(distinct mp.id) as missoes,
        count(distinct mm.id) as mencoes
    from public.funcionarios f
    left join public.equipamento_alocacoes ea on ea.funcionario_id = f.id
    left join public.licencas_operacao lo on lo.funcionario_id = f.id
    left join public.missao_participantes mp on mp.funcionario_id = f.id
    left join public.mencoes_midia mm on mm.funcionario_id = f.id
    where f.ativo = true
    group by f.id
)
select
    count(*) as ativos,
    min(equipamentos_ativos) as min_equipamentos_ativos,
    min(licencas) as min_licencas,
    min(missoes) as min_missoes,
    min(mencoes) as min_mencoes,
    count(*) filter (
        where equipamentos_ativos = 0
           or licencas = 0
           or missoes = 0
           or mencoes < 4
    ) as ativos_com_lacuna
from active_coverage;
