-- Read-only, server-authoritative fact model for extraordinary-benefit preflight.

create extension if not exists btree_gist;

create table if not exists public.extraordinary_benefit_program_policy (
    id bigserial primary key,
    reason_code varchar(40) not null,
    version bigint not null,
    effective_from date not null,
    effective_until date,
    active boolean not null,
    maximum_amount numeric(15, 2) not null,
    customer_additional_eligible boolean,
    available_budget_amount numeric(15, 2) not null,
    constraint uq_extraordinary_benefit_program_policy unique (reason_code, version),
    constraint ck_extraordinary_benefit_program_policy_reason check (
        reason_code in ('EMERGENCY_MEDICAL','FAMILY_HARDSHIP','DISASTER_RECOVERY','OTHER')),
    constraint ck_extraordinary_benefit_program_policy_validity check (
        effective_until is null or effective_until > effective_from),
    constraint ck_extraordinary_benefit_program_policy_amounts check (
        maximum_amount > 0 and available_budget_amount >= 0),
    constraint ex_extraordinary_benefit_program_policy_no_overlap
        exclude using gist (
            reason_code with =,
            daterange(effective_from, coalesce(effective_until, 'infinity'::date), '[)') with &&
        )
);

create table if not exists public.extraordinary_benefit_payment_window (
    policy_id bigint not null references public.extraordinary_benefit_program_policy(id) on delete restrict,
    payment_date date not null,
    enabled boolean not null default true,
    primary key (policy_id, payment_date)
);

create table if not exists public.extraordinary_benefit_grant_history (
    id bigserial primary key,
    worker_id bigint not null references public.funcionarios(id) on delete restrict,
    reason_code varchar(40) not null,
    event_date date not null,
    status varchar(20) not null,
    source_reference varchar(120) not null,
    constraint uq_extraordinary_benefit_grant_history_source unique (source_reference),
    constraint ck_extraordinary_benefit_grant_history_status check (
        status in ('REQUESTED','APPROVED','APPLIED','REVERSED'))
);

create index if not exists idx_extraordinary_benefit_grant_history_duplicate
    on public.extraordinary_benefit_grant_history (worker_id, reason_code, event_date, status);

insert into public.extraordinary_benefit_program_policy (
    reason_code, version, effective_from, effective_until, active,
    maximum_amount, customer_additional_eligible, available_budget_amount)
values
    ('EMERGENCY_MEDICAL', 1, date '2026-01-01', null, true, 5000.00, true, 100000.00),
    ('FAMILY_HARDSHIP', 1, date '2026-01-01', null, true, 3500.00, true, 75000.00),
    ('DISASTER_RECOVERY', 1, date '2026-01-01', null, true, 8000.00, true, 150000.00),
    ('OTHER', 1, date '2026-01-01', null, false, 1000.00, null, 0.00)
on conflict (reason_code, version) do nothing;

insert into public.extraordinary_benefit_payment_window (policy_id, payment_date, enabled)
select policy.id, dates.payment_date, true
from public.extraordinary_benefit_program_policy policy
cross join (values (date '2026-07-20'), (date '2026-07-31'), (date '2026-08-20')) dates(payment_date)
where policy.version = 1
on conflict (policy_id, payment_date) do nothing;

-- The migration owner remains the DDL owner. The API role receives only the reads required by
-- evaluate-authoritative; write authority must be introduced separately with transactional controls.
grant select on table
    public.extraordinary_benefit_program_policy,
    public.extraordinary_benefit_payment_window,
    public.extraordinary_benefit_grant_history
to ${OPERATIONAL_RUNTIME_ROLE};
