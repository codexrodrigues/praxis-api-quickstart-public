alter table public.procurement_purchase_orders
    add column if not exists status varchar(40) not null default 'DRAFT';

alter table public.procurement_purchase_orders
    add column if not exists disabled_reason varchar(255);

alter table public.procurement_purchase_orders
    add column if not exists approved_at date;

alter table public.procurement_purchase_orders
    add column if not exists cancelled_at date;

alter table public.procurement_purchase_orders
    add column if not exists received_at date;

create index if not exists idx_procurement_purchase_orders_status
    on public.procurement_purchase_orders(status);
