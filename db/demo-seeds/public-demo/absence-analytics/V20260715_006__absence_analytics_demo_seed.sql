-- Public-demo-only data for issue #98. This is not a migration of corporate history.
-- The assignments are explicitly authored for the public demo identities and use
-- half-open validity intervals: [effective_from, effective_to).
--
-- This query is intentionally the first executable statement. JdbcSqlRunner executes all files
-- in one transaction, so a missing or mismatched demo fingerprint aborts before any assignment
-- or absence can be inserted. The roster check also prevents treating reused corporate IDs as
-- the public demo merely because a table happens to contain rows 1..50.
do $$
begin
    if not (
        exists (
            select 1
            from public.praxis_demo_dataset_guard guard
            where guard.dataset_key = 'praxis-public-demo'
              and guard.dataset_fingerprint = 'praxis-public-demo-2026-07-15'
        )
        and (select count(*) from public.funcionarios where id between 1 and 50) = 50
        and (select count(*) from public.departamentos where id between 1 and 27) = 27
        and exists (
            select 1 from public.funcionarios
            where id = 1
              and nome_completo = 'Tony Stark'
              and cpf = '90000000175'
              and email = 'tony.stark@stark.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.funcionarios
            where id = 25
              and nome_completo = 'Alyx Vance'
              and cpf = '90000002542'
              and email = 'alyx.vance@blackmesa.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.funcionarios
            where id = 50
              and nome_completo = 'Maria Hill'
              and cpf = '90000005053'
              and email = 'maria.hill@shield.demo.praxisui.dev'
        )
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: expected public demo fingerprint and recognized employee identities; no seed was written.';
    end if;
end
$$;

with demo_assignments (funcionario_id, departamento_id, effective_from, effective_to) as (
    values
        (1, 1, date '2022-01-01', null),
        (2, 1, date '2022-01-01', date '2026-06-15'),
        (2, 3, date '2026-06-15', null),
        (3, 4, date '2022-01-01', date '2026-07-10'),
        (3, 5, date '2026-07-10', null),
        (4, 5, date '2022-01-01', date '2026-07-10'),
        (4, 4, date '2026-07-10', null),
        (5, 7, date '2022-01-01', null),
        (6, 6, date '2022-01-01', null),
        (7, 8, date '2022-01-01', null),
        (8, 8, date '2022-01-01', null),
        (9, 11, date '2022-01-01', null),
        (10, 10, date '2022-01-01', null),
        (11, 11, date '2022-01-01', null),
        (12, 11, date '2022-01-01', null),
        (13, 13, date '2022-01-01', null),
        (14, 12, date '2022-01-01', null),
        (15, 13, date '2022-01-01', null),
        (16, 12, date '2022-01-01', null),
        (17, 14, date '2022-01-01', null),
        (18, 14, date '2022-01-01', null),
        (19, 16, date '2022-01-01', null),
        (20, 16, date '2022-01-01', null),
        (21, 16, date '2022-01-01', null),
        (22, 19, date '2022-01-01', null),
        (23, 18, date '2022-01-01', null),
        (24, 22, date '2022-01-01', null),
        (25, 22, date '2022-01-01', null),
        (26, 24, date '2022-01-01', null),
        (27, 20, date '2022-01-01', null),
        (28, 20, date '2022-01-01', null),
        (29, 2, date '2022-01-01', null),
        (30, 23, date '2022-01-01', null),
        (31, 1, date '2022-01-01', null),
        (32, 5, date '2022-01-01', null),
        (33, 4, date '2022-01-01', null),
        (34, 19, date '2022-01-01', null),
        (35, 4, date '2022-01-01', null),
        (36, 10, date '2022-01-01', null),
        (37, 26, date '2022-01-01', null),
        (38, 9, date '2022-01-01', null),
        (39, 10, date '2022-01-01', null),
        (40, 21, date '2022-01-01', null),
        (41, 21, date '2022-01-01', null),
        (42, 6, date '2022-01-01', null),
        (43, 18, date '2022-01-01', null),
        (44, 6, date '2022-01-01', null),
        (45, 10, date '2022-01-01', null),
        (46, 14, date '2022-01-01', null),
        (47, 2, date '2022-01-01', null),
        (48, 7, date '2022-01-01', null),
        (49, 17, date '2022-01-01', null),
        (50, 2, date '2022-01-01', null)
)
insert into public.funcionario_lotacoes_departamento (
    funcionario_id,
    departamento_id,
    effective_from,
    effective_to
)
select
    da.funcionario_id,
    da.departamento_id,
    da.effective_from,
    da.effective_to
from demo_assignments da
where not exists (
    select 1
    from public.funcionario_lotacoes_departamento existing
    where existing.funcionario_id = da.funcionario_id
      and existing.departamento_id = da.departamento_id
      and existing.effective_from = da.effective_from
      and existing.effective_to is not distinct from da.effective_to
);

-- Current-period examples cover comparison, month boundaries, transfer attribution,
-- all criticality levels and de-duplication of overlapping source events.
insert into public.ferias_afastamentos (
    tipo,
    data_inicio,
    data_fim,
    observacoes,
    funcionario_id
)
select
    demo_event.tipo,
    demo_event.data_inicio,
    demo_event.data_fim,
    'PRAXIS_DEMO_HR_ANALYTICS_20260715',
    demo_event.funcionario_id
from (
    values
        ('FERIAS', date '2026-06-03', date '2026-06-07', 1),
        ('AFASTAMENTO', date '2026-06-10', date '2026-06-20', 2),
        ('FERIAS', date '2026-07-01', date '2026-07-18', 3),
        ('AFASTAMENTO', date '2026-07-06', date '2026-07-14', 4),
        ('FERIAS', date '2026-06-04', date '2026-06-12', 5),
        ('AFASTAMENTO', date '2026-07-01', date '2026-07-20', 6),
        ('FERIAS', date '2026-06-28', date '2026-07-04', 7),
        ('AFASTAMENTO', date '2026-06-05', date '2026-06-10', 8),
        ('AFASTAMENTO', date '2026-06-08', date '2026-06-15', 8),
        ('FERIAS', date '2026-07-01', date '2026-07-06', 9),
        ('FERIAS', date '2026-06-01', date '2026-06-05', 10)
) as demo_event(tipo, data_inicio, data_fim, funcionario_id)
where not exists (
    select 1
    from public.ferias_afastamentos existing
    where existing.funcionario_id = demo_event.funcionario_id
      and existing.tipo = demo_event.tipo
      and existing.data_inicio = demo_event.data_inicio
      and existing.data_fim = demo_event.data_fim
      and existing.observacoes = 'PRAXIS_DEMO_HR_ANALYTICS_20260715'
);

-- Expected proof on a pristine public demo database after this seed:
-- * 53 effective assignments for employees 1..50;
-- * all 100 pre-existing absence events become attributable;
-- * 11 current-period events exercise comparison and policy thresholds;
-- * employee 8 has 11 unique June days despite two overlapping source events.
