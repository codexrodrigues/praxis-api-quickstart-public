-- Isola retries por alvo e ator autorizado. A chave continua opaca para o dominio.

alter table public.praxis_resource_action_execution
    add column if not exists resource_id varchar(128) not null default '__collection__';

alter table public.praxis_resource_action_execution
    drop constraint if exists uq_praxis_resource_action_execution_idempotency;

alter table public.praxis_resource_action_execution
    add constraint uq_praxis_resource_action_execution_idempotency
        unique (resource_key, resource_id, action_id, actor_subject, idempotency_key);

create index if not exists idx_praxis_resource_action_execution_resource
    on public.praxis_resource_action_execution (resource_key, resource_id, started_at desc);

drop index if exists public.uq_praxis_resource_action_transition_idempotency;

create unique index uq_praxis_resource_action_transition_idempotency
    on public.praxis_resource_action_transition
        (resource_key, resource_id, action_id, actor_subject, idempotency_key)
    where idempotency_key is not null;
