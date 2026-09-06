-- P2F-ADR-10: outbox transacional para a unidade STATEMENT_ATOMIC do Rule Lab.
-- O payload contém apenas IDs e evidência do snapshot; dados de negócio devem ser recarregados
-- pelo consumidor sob sua própria autorização.

create table if not exists public.extraordinary_benefit_statement_outbox (
    message_id uuid primary key,
    operation_id uuid not null,
    event_type varchar(160) not null,
    tenant_id varchar(120) not null,
    environment varchar(80) not null,
    correlation_id varchar(255) not null,
    payload jsonb not null,
    delivery_status varchar(32) not null,
    delivery_attempts integer not null default 0,
    next_attempt_at timestamptz not null,
    next_reconciliation_at timestamptz not null,
    lease_token uuid,
    lease_until timestamptz,
    created_at timestamptz not null,
    delivered_at timestamptz,
    last_failure_code varchar(120),
    last_failure_message varchar(1000),
    last_failure_at timestamptz,
    constraint uq_extraordinary_benefit_statement_outbox_operation unique (operation_id),
    constraint ck_extraordinary_benefit_statement_outbox_status
        check (delivery_status in ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD_LETTER')),
    constraint ck_extraordinary_benefit_statement_outbox_attempts
        check (delivery_attempts >= 0),
    constraint ck_extraordinary_benefit_statement_outbox_lease
        check ((delivery_status = 'PROCESSING' and lease_token is not null and lease_until is not null)
            or (delivery_status <> 'PROCESSING' and lease_token is null and lease_until is null))
);

create index if not exists idx_extraordinary_benefit_statement_outbox_dispatch
    on public.extraordinary_benefit_statement_outbox
        (delivery_status, next_attempt_at, created_at);

create index if not exists idx_extraordinary_benefit_statement_outbox_lease
    on public.extraordinary_benefit_statement_outbox
        (delivery_status, lease_until)
    where delivery_status = 'PROCESSING';

create index if not exists idx_extraordinary_benefit_statement_outbox_reconciliation
    on public.extraordinary_benefit_statement_outbox
        (next_reconciliation_at, created_at)
    where delivery_status <> 'DELIVERED';

-- Trilha append-only do ato administrativo de replay. Não contém payload ou resposta externa.
create table if not exists public.extraordinary_benefit_statement_replay_audit (
    audit_id uuid primary key,
    message_id uuid not null,
    requested_at timestamptz not null,
    actor_subject varchar(255) not null,
    justification varchar(1000) not null,
    correlation_id varchar(255) not null,
    expected_failure_code varchar(120),
    observed_failure_code varchar(120),
    replay_outcome varchar(80) not null,
    acknowledged_at timestamptz,
    constraint ck_extraordinary_benefit_statement_replay_outcome check (replay_outcome in (
        'REPLAY_SCHEDULED', 'ACKNOWLEDGED_NO_REPLAY', 'REJECTED_MESSAGE_NOT_FOUND',
        'REJECTED_NOT_DEAD_LETTER', 'REJECTED_QUARANTINE', 'REJECTED_FAILURE_CHANGED',
        'REJECTED_NO_PROBE', 'REJECTED_PROBE_FAILED'))
);

create index if not exists idx_extraordinary_benefit_statement_replay_audit_message
    on public.extraordinary_benefit_statement_replay_audit (message_id, requested_at);
