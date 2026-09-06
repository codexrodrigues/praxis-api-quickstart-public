-- Persisted resource version for governed regulatory-agreement item actions.
-- The runtime user must not execute this DDL; apply it with the operational migration identity.

alter table public.acordos_regulatorios
    add column if not exists version bigint not null default 0;
