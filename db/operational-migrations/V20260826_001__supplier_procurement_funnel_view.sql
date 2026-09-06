-- Cumulative supplier funnel for dashboard/chart/table materialization.
-- Every stage is derived from persisted operational facts. The `eligible` stage means
-- persisted operational eligibility and does not copy the governed dynamic selection policy.

create or replace view public.vw_supplier_procurement_funnel as
with supplier_facts as (
    select
        s.company_id,
        s.id as supplier_id,
        true as identified,
        (s.homologation_status = 'APPROVED') as homologated,
        (s.homologation_status = 'APPROVED' and s.status in ('ACTIVE', 'RESTRICTED')) as eligible,
        (
            s.homologation_status = 'APPROVED'
            and s.status in ('ACTIVE', 'RESTRICTED')
            and exists (
                select 1
                from public.procurement_contracts c
                where c.company_id = s.company_id
                  and c.supplier_id = s.id
                  and c.status in ('ACTIVE', 'SIGNED')
            )
        ) as contracted,
        (
            s.homologation_status = 'APPROVED'
            and s.status in ('ACTIVE', 'RESTRICTED')
            and exists (
                select 1
                from public.procurement_contracts c
                join public.procurement_purchase_orders po
                  on po.company_id = c.company_id
                 and po.supplier_id = c.supplier_id
                 and po.contract_id = c.id
                 and po.status in ('APPROVED', 'RECEIVED')
                where c.company_id = s.company_id
                  and c.supplier_id = s.id
                  and c.status in ('ACTIVE', 'SIGNED')
            )
        ) as ordered,
        (
            s.homologation_status = 'APPROVED'
            and s.status in ('ACTIVE', 'RESTRICTED')
            and exists (
                select 1
                from public.procurement_contracts c
                join public.procurement_purchase_orders po
                  on po.company_id = c.company_id
                 and po.supplier_id = c.supplier_id
                 and po.contract_id = c.id
                 and po.status = 'RECEIVED'
                 and po.received_at is not null
                where c.company_id = s.company_id
                  and c.supplier_id = s.id
                  and c.status in ('ACTIVE', 'SIGNED')
            )
        ) as received
    from public.procurement_suppliers s
), stage_catalog(stage_order, stage_key, stage_label) as (
    values
        (1, 'identified', 'Identificados'),
        (2, 'homologated', 'Homologados'),
        (3, 'eligible', 'Elegíveis Operacionais'),
        (4, 'contracted', 'Contratados'),
        (5, 'ordered', 'Com Pedido Aprovado'),
        (6, 'received', 'Com Recebimento')
), stage_volumes as (
    select
        c.id as company_id,
        c.legal_name as company_name,
        sc.stage_order,
        sc.stage_key,
        sc.stage_label,
        count(sf.supplier_id) filter (
            where case sc.stage_key
                when 'identified' then sf.identified
                when 'homologated' then sf.homologated
                when 'eligible' then sf.eligible
                when 'contracted' then sf.contracted
                when 'ordered' then sf.ordered
                when 'received' then sf.received
                else false
            end
        )::bigint as volume
    from public.procurement_companies c
    cross join stage_catalog sc
    left join supplier_facts sf on sf.company_id = c.id
    group by c.id, c.legal_name, sc.stage_order, sc.stage_key, sc.stage_label
), with_previous as (
    select
        sv.*,
        lag(sv.volume) over (partition by sv.company_id order by sv.stage_order) as previous_volume
    from stage_volumes sv
)
select
    concat(company_id, ':', stage_order) as analytics_id,
    company_id,
    company_name,
    stage_order,
    stage_key,
    stage_label,
    volume,
    previous_volume,
    case
        when previous_volume is null then 1.000000::numeric(12, 6)
        when previous_volume = 0 then 0.000000::numeric(12, 6)
        else round(volume::numeric / previous_volume::numeric, 6)
    end as conversion_rate,
    greatest(coalesce(previous_volume, volume) - volume, 0)::bigint as drop_off_count,
    case
        when previous_volume is null or previous_volume = 0 then 0.000000::numeric(12, 6)
        else round(greatest(previous_volume - volume, 0)::numeric / previous_volume::numeric, 6)
    end as drop_off_rate
from with_previous;

grant select on table public.vw_supplier_procurement_funnel to ${OPERATIONAL_RUNTIME_ROLE};
