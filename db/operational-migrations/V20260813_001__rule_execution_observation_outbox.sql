-- Durable, redacted bridge from the operational runtime to the Config control plane.
-- Facts, business identifiers, reason codes and executable payloads are deliberately excluded.
create table if not exists public.rule_execution_observation_outbox (
    observation_id uuid primary key,
    tenant_id varchar(128) not null,
    environment varchar(128) not null,
    snapshot_key varchar(128) not null,
    snapshot_content_hash varchar(64) not null,
    activation_revision bigint not null,
    outcome varchar(32) not null,
    duration_micros bigint not null,
    observed_at timestamptz not null,
    delivery_status varchar(32) not null default 'PENDING',
    delivery_attempts integer not null default 0,
    next_attempt_at timestamptz not null,
    lease_token uuid,
    lease_until timestamptz,
    created_at timestamptz not null,
    delivered_at timestamptz,
    last_failure_code varchar(120),
    constraint ck_rule_execution_observation_outbox_hash
        check (snapshot_content_hash ~ '^[A-F0-9]{64}$'),
    constraint ck_rule_execution_observation_outbox_revision check (activation_revision > 0),
    constraint ck_rule_execution_observation_outbox_duration check (duration_micros between 0 and 300000000),
    constraint ck_rule_execution_observation_outbox_outcome
        check (outcome in ('ALLOW', 'DENY', 'NOT_APPLICABLE', 'INCONCLUSIVE', 'TECHNICAL_ERROR')),
    constraint ck_rule_execution_observation_outbox_status
        check (delivery_status in ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD_LETTER')),
    constraint ck_rule_execution_observation_outbox_lease
        check ((delivery_status = 'PROCESSING' and lease_token is not null and lease_until is not null)
            or (delivery_status <> 'PROCESSING' and lease_token is null and lease_until is null))
);

create index if not exists idx_rule_execution_observation_outbox_dispatch
    on public.rule_execution_observation_outbox
        (delivery_status, next_attempt_at, created_at, observation_id)
    where delivery_status = 'PENDING';

create index if not exists idx_rule_execution_observation_outbox_expired_lease
    on public.rule_execution_observation_outbox
        (delivery_status, lease_until, created_at, observation_id)
    where delivery_status = 'PROCESSING';
