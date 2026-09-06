-- G2 HR analytics: read-only absence projection for governed aggregate comparison.
-- The view intentionally omits absence type, notes, employee name and the employee's current department.
-- Criticality policy v1: STANDARD < 7 days, ATTENTION 7-14 days, CRITICAL >= 15 days.
-- DROP is required because PostgreSQL CREATE OR REPLACE VIEW cannot insert/reorder output columns.

drop view if exists public.vw_analytics_afastamentos;

create view public.vw_analytics_afastamentos as
with absence_months as (
    select
        fa.id as afastamento_id,
        fa.funcionario_id,
        gs.competencia::date as competencia,
        greatest(fa.data_inicio, gs.competencia::date) as periodo_inicio,
        least(fa.data_fim, (gs.competencia + interval '1 month - 1 day')::date) as periodo_fim
    from public.ferias_afastamentos fa
    cross join lateral generate_series(
        date_trunc('month', fa.data_inicio)::date,
        date_trunc('month', fa.data_fim)::date,
        interval '1 month'
    ) as gs(competencia)
),
attributed as (
    select
        am.afastamento_id,
        am.funcionario_id,
        ld.departamento_id,
        d.codigo as departamento_codigo,
        d.nome as departamento,
        am.competencia,
        extract(year from am.competencia)::integer as ano,
        extract(month from am.competencia)::integer as mes,
        greatest(am.periodo_inicio, ld.effective_from) as periodo_inicio,
        least(am.periodo_fim, coalesce(ld.effective_to - 1, am.periodo_fim)) as periodo_fim
    from absence_months am
    join public.funcionario_lotacoes_departamento ld
        on ld.funcionario_id = am.funcionario_id
       and ld.effective_from <= am.periodo_fim
       and coalesce(ld.effective_to, 'infinity'::date) > am.periodo_inicio
    join public.departamentos d
        on d.id = ld.departamento_id
)
select
    concat(afastamento_id, ':', funcionario_id, ':', departamento_id, ':', to_char(competencia, 'YYYYMM'), ':', periodo_inicio) as analytics_id,
    afastamento_id,
    funcionario_id,
    departamento_id,
    departamento_codigo,
    departamento,
    competencia,
    ano,
    mes,
    periodo_inicio,
    periodo_fim,
    (periodo_fim - periodo_inicio + 1)::bigint as dias_afastado,
    case
        when (periodo_fim - periodo_inicio + 1) >= 15 then 'CRITICAL'
        when (periodo_fim - periodo_inicio + 1) >= 7 then 'ATTENTION'
        else 'STANDARD'
    end as criticality_level,
    'hr-absence-criticality-v1' as criticality_policy_id,
    '2026-07-14' as criticality_policy_version
from attributed
where periodo_inicio <= periodo_fim;

grant select on table public.vw_analytics_afastamentos to ${OPERATIONAL_RUNTIME_ROLE};
