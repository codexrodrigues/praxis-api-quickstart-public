-- FND-06 reference boundary for server-side rule facts.
-- Rows are scoped, versioned and temporal; the runtime receives SELECT only.
-- This table contains fictional laboratory facts and is not an Ergon parity baseline.

create table if not exists public.rule_lab_authoritative_benefit_facts (
    tenant_id varchar(120) not null,
    environment varchar(80) not null,
    organization_key varchar(120) not null,
    fact_reference varchar(120) not null,
    source_system varchar(120) not null,
    source_record_digest varchar(64) not null,
    source_version bigint not null,
    effective_from timestamp with time zone not null,
    effective_to timestamp with time zone,
    worker_status varchar(20) not null,
    duplicate_grant boolean not null,
    program_active boolean not null,
    program_maximum_amount numeric(15, 2) not null,
    customer_additional_eligible boolean,
    available_budget_amount numeric(15, 2) not null,
    recorded_at timestamp with time zone not null,
    constraint pk_rule_lab_authoritative_benefit_facts primary key
        (tenant_id, environment, organization_key, fact_reference, source_version),
    constraint ck_rule_lab_authoritative_fact_version check (source_version > 0),
    constraint ck_rule_lab_authoritative_fact_digest check (source_record_digest ~ '^[0-9A-F]{64}$'),
    constraint ck_rule_lab_authoritative_fact_interval check
        (effective_to is null or effective_to > effective_from),
    constraint ck_rule_lab_authoritative_worker_status check
        (worker_status in ('ACTIVE', 'LEAVE', 'TERMINATED')),
    constraint ck_rule_lab_authoritative_program_maximum check (program_maximum_amount > 0),
    constraint ck_rule_lab_authoritative_budget check (available_budget_amount >= 0)
);

create index if not exists idx_rule_lab_authoritative_benefit_fact_lookup
    on public.rule_lab_authoritative_benefit_facts
       (tenant_id, environment, organization_key, fact_reference, effective_from, effective_to);

create table if not exists public.rule_lab_authoritative_benefit_payment_date (
    tenant_id varchar(120) not null,
    environment varchar(80) not null,
    organization_key varchar(120) not null,
    fact_reference varchar(120) not null,
    source_version bigint not null,
    allowed_payment_date date not null,
    constraint pk_rule_lab_authoritative_benefit_payment_date primary key
        (tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date),
    constraint fk_rule_lab_authoritative_benefit_payment_date foreign key
        (tenant_id, environment, organization_key, fact_reference, source_version)
        references public.rule_lab_authoritative_benefit_facts
        (tenant_id, environment, organization_key, fact_reference, source_version)
        on delete cascade
);

revoke all on public.rule_lab_authoritative_benefit_facts from public;
revoke all on public.rule_lab_authoritative_benefit_payment_date from public;

-- Role names are environment-owned. After applying this migration, the deployment pipeline must
-- grant SELECT on both tables to the concrete application runtime role; never grant write access.
