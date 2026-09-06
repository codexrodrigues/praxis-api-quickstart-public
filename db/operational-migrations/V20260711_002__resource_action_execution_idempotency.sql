-- Execução idempotente de actions de coleção.
-- Diferente da tabela de transições, este registro representa uma única requisição de negócio.

create table if not exists public.praxis_resource_action_execution (
    execution_id uuid primary key,
    resource_key varchar(200) not null,
    action_id varchar(120) not null,
    action_scope varchar(32) not null,
    idempotency_key varchar(255) not null,
    request_hash varchar(128) not null,
    execution_status varchar(32) not null,
    response_payload jsonb,
    correlation_id varchar(255) not null,
    request_id varchar(255),
    actor_subject varchar(255) not null,
    actor_authorities text,
    started_at timestamptz not null,
    completed_at timestamptz,
    failure_code varchar(120),
    failure_message text,
    constraint uq_praxis_resource_action_execution_idempotency
        unique (resource_key, action_id, idempotency_key)
);

create index if not exists idx_praxis_resource_action_execution_correlation
    on public.praxis_resource_action_execution (correlation_id);
