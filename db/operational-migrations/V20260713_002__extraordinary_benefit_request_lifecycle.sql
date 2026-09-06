-- QL-05: agregado persistente do beneficio extraordinario e ledger do efeito executado pelo host.

create table if not exists public.extraordinary_benefit_request (
    id bigserial primary key,
    request_reference varchar(80) not null,
    reason_code varchar(40) not null,
    event_date date not null,
    requested_amount numeric(15, 2) not null,
    worker_status varchar(20) not null,
    duplicate_grant boolean not null,
    program_active boolean not null,
    program_maximum_amount numeric(15, 2) not null,
    customer_additional_eligible boolean,
    requested_payment_date date not null,
    allowed_payment_dates varchar(1000) not null,
    available_budget_amount numeric(15, 2) not null,
    user_time_zone varchar(80) not null,
    lifecycle_status varchar(20) not null,
    recommended_amount numeric(15, 2) not null,
    currency varchar(3) not null,
    snapshot_key varchar(200) not null,
    snapshot_content_hash varchar(64) not null,
    snapshot_activation_revision bigint not null,
    rule_set_key varchar(200) not null,
    rule_set_version integer not null,
    facts_digest varchar(64) not null,
    plan_digest varchar(64) not null,
    planned_effect_intent varchar(120) not null,
    evaluation_business_message varchar(1000) not null,
    evaluation_reason_codes varchar(1000) not null,
    effect_status varchar(20) not null,
    evaluated_at timestamptz not null,
    submitted_at timestamptz,
    approved_at timestamptz,
    applied_at timestamptz,
    created_by varchar(255) not null,
    last_transition_by varchar(255) not null,
    version bigint not null default 0,
    constraint uq_extraordinary_benefit_request_reference unique (request_reference),
    constraint ck_extraordinary_benefit_request_lifecycle
        check (lifecycle_status in ('EVALUATED', 'SUBMITTED', 'APPROVED', 'APPLIED')),
    constraint ck_extraordinary_benefit_request_effect
        check (effect_status in ('PLANNED', 'EXECUTED'))
);

create index if not exists idx_extraordinary_benefit_request_queue
    on public.extraordinary_benefit_request (lifecycle_status, evaluated_at desc);

create table if not exists public.extraordinary_benefit_grant_effect (
    id bigserial primary key,
    effect_execution_id uuid not null,
    benefit_request_id bigint not null,
    request_reference varchar(80) not null,
    intent_type varchar(120) not null,
    amount numeric(15, 2) not null,
    currency varchar(3) not null,
    executed_at timestamptz not null,
    executed_by varchar(255) not null,
    constraint uq_extraordinary_benefit_effect_execution unique (effect_execution_id),
    constraint uq_extraordinary_benefit_effect_request unique (benefit_request_id),
    constraint fk_extraordinary_benefit_effect_request
        foreign key (benefit_request_id)
        references public.extraordinary_benefit_request (id)
        on delete restrict
);

create index if not exists idx_extraordinary_benefit_effect_executed_at
    on public.extraordinary_benefit_grant_effect (executed_at desc);
