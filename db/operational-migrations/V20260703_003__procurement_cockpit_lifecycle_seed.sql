with lifecycle(id, status, disabled_reason, approved_at, cancelled_at, received_at) as (
    values
        (40, 'DRAFT', null, null::date, null::date, null::date),
        (41, 'APPROVED', null, date '2026-04-17', null::date, null::date),
        (42, 'RECEIVED', null, date '2026-04-19', null::date, date '2026-04-22'),
        (43, 'CANCELLED', 'Demanda cancelada pela area solicitante', null::date, date '2026-04-21', null::date),
        (44, 'APPROVED', null, date '2026-04-18', null::date, null::date),
        (45, 'DRAFT', null, null::date, null::date, null::date),
        (46, 'APPROVED', null, date '2026-04-22', null::date, null::date),
        (47, 'RECEIVED', null, date '2026-04-20', null::date, date '2026-04-24'),
        (48, 'DRAFT', null, null::date, null::date, null::date),
        (49, 'CANCELLED', 'Fornecedor solicitou replanejamento logistico', null::date, date '2026-04-25', null::date)
)
update public.procurement_purchase_orders po
set status = lifecycle.status,
    disabled_reason = lifecycle.disabled_reason,
    approved_at = lifecycle.approved_at,
    cancelled_at = lifecycle.cancelled_at,
    received_at = lifecycle.received_at
from lifecycle
where po.id = lifecycle.id
  and po.order_date between date '2026-04-15' and date '2026-04-25';
