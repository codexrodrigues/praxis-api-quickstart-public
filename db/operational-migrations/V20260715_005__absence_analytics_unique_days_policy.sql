-- G2 HR analytics: one public row per employee, effective department and calendar month.
-- The policy function is the sole executable threshold owner for this projection.

drop view if exists public.vw_analytics_afastamentos;

create or replace function public.hr_absence_criticality_level(unique_absence_days bigint)
returns text
language sql
immutable
parallel safe
as $$
    select case
        when unique_absence_days >= 15 then 'CRITICAL'
        when unique_absence_days >= 7 then 'ATTENTION'
        else 'STANDARD'
    end
$$;

create view public.vw_analytics_afastamentos as
with assigned_absence_days as (
    select distinct
        fa.funcionario_id,
        ld.departamento_id,
        d.codigo as departamento_codigo,
        d.nome as departamento,
        date_trunc('month', gs.dia)::date as competencia,
        gs.dia::date as dia_afastado
    from public.ferias_afastamentos fa
    cross join lateral generate_series(
        fa.data_inicio::timestamp,
        fa.data_fim::timestamp,
        interval '1 day'
    ) as gs(dia)
    join public.funcionario_lotacoes_departamento ld
        on ld.funcionario_id = fa.funcionario_id
       and ld.effective_from <= gs.dia::date
       and coalesce(ld.effective_to, 'infinity'::date) > gs.dia::date
    join public.departamentos d
        on d.id = ld.departamento_id
    where fa.data_fim >= fa.data_inicio
), aggregated as (
    select
        funcionario_id,
        departamento_id,
        departamento_codigo,
        departamento,
        competencia,
        extract(year from competencia)::integer as ano,
        extract(month from competencia)::integer as mes,
        min(dia_afastado) as periodo_inicio,
        max(dia_afastado) as periodo_fim,
        count(*)::bigint as dias_afastado
    from assigned_absence_days
    group by funcionario_id, departamento_id, departamento_codigo, departamento, competencia
)
select
    concat(funcionario_id, ':', departamento_id, ':', to_char(competencia, 'YYYYMM')) as analytics_id,
    funcionario_id,
    departamento_id,
    departamento_codigo,
    departamento,
    competencia,
    ano,
    mes,
    periodo_inicio,
    periodo_fim,
    dias_afastado,
    public.hr_absence_criticality_level(dias_afastado) as criticality_level,
    'hr-absence-criticality-v1' as criticality_policy_id,
    '2026-07-15' as criticality_policy_version
from aggregated;

-- Recreating a view drops its prior grants; restore the least-privilege runtime access explicitly.
grant execute on function public.hr_absence_criticality_level(bigint) to ${OPERATIONAL_RUNTIME_ROLE};
grant select on public.vw_analytics_afastamentos to ${OPERATIONAL_RUNTIME_ROLE};
