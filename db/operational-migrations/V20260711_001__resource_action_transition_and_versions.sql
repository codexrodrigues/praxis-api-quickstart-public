-- Baseline de persistencia para resource workflow actions auditaveis.
-- O estado corrente continua nos agregados; esta tabela registra somente transicoes confirmadas.

alter table public.funcionarios
    add column if not exists version bigint not null default 0;

alter table public.eventos_folha
    add column if not exists version bigint not null default 0;

create table if not exists public.praxis_resource_action_transition (
    transition_id uuid primary key,
    resource_key varchar(200) not null,
    resource_id varchar(128) not null,
    action_id varchar(120) not null,
    action_scope varchar(32) not null,
    previous_state varchar(120),
    resulting_state varchar(120),
    reason_code varchar(120),
    comment text,
    effective_at date not null,
    performed_at timestamptz not null,
    actor_subject varchar(255) not null,
    actor_authorities text,
    correlation_id varchar(255) not null,
    request_id varchar(255),
    idempotency_key varchar(255),
    version_before bigint,
    version_after bigint,
    metadata jsonb
);

create index if not exists idx_praxis_resource_action_transition_resource
    on public.praxis_resource_action_transition
        (resource_key, resource_id, performed_at desc);

create index if not exists idx_praxis_resource_action_transition_correlation
    on public.praxis_resource_action_transition (correlation_id);

create unique index if not exists uq_praxis_resource_action_transition_idempotency
    on public.praxis_resource_action_transition
        (resource_key, resource_id, action_id, idempotency_key)
    where idempotency_key is not null;
