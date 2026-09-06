-- P2F-ADR-12: governed retention and legal hold for redacted rule audit ledgers.
-- Runtime identities must not own these objects. Deployment grants are intentionally environment-specific.

create table if not exists public.praxis_rule_audit_retention_guard (
    transaction_id bigint not null,
    ledger_name varchar(120) not null,
    retention_run_id uuid not null,
    primary key (transaction_id, ledger_name),
    constraint ck_praxis_rule_audit_retention_guard_ledger check (ledger_name in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit'))
);

create table if not exists public.praxis_rule_audit_retention_run (
    retention_run_id uuid primary key,
    ledger_name varchar(120) not null,
    policy_key varchar(160) not null,
    cutoff_utc timestamptz not null,
    batch_size integer not null,
    deleted_rows integer not null,
    authorization_reference_digest char(64) not null,
    executed_at timestamptz not null,
    constraint ck_praxis_rule_audit_retention_run_ledger check (ledger_name in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit')),
    constraint ck_praxis_rule_audit_retention_run_batch check (batch_size between 1 and 10000),
    constraint ck_praxis_rule_audit_retention_run_deleted check (deleted_rows between 0 and batch_size),
    constraint ck_praxis_rule_audit_retention_run_authorization check (
        authorization_reference_digest ~ '^[A-F0-9]{64}$')
);

create index if not exists idx_praxis_rule_audit_retention_run_ledger
    on public.praxis_rule_audit_retention_run (ledger_name, executed_at);

create table if not exists public.praxis_rule_audit_legal_hold (
    ledger_name varchar(120) not null,
    record_id uuid not null,
    hold_id uuid not null unique,
    reason_code varchar(120) not null,
    authorization_reference_digest char(64) not null,
    placed_at timestamptz not null,
    primary key (ledger_name, record_id),
    constraint ck_praxis_rule_audit_legal_hold_ledger check (ledger_name in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit')),
    constraint ck_praxis_rule_audit_legal_hold_authorization check (
        authorization_reference_digest ~ '^[A-F0-9]{64}$')
);

create table if not exists public.praxis_rule_audit_legal_hold_event (
    event_id uuid primary key,
    hold_id uuid not null,
    ledger_name varchar(120) not null,
    record_id uuid not null,
    hold_action varchar(16) not null,
    reason_code varchar(120) not null,
    authorization_reference_digest char(64) not null,
    occurred_at timestamptz not null,
    constraint ck_praxis_rule_audit_legal_hold_event_ledger check (ledger_name in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit')),
    constraint ck_praxis_rule_audit_legal_hold_event_action check (hold_action in ('PLACE', 'RELEASE')),
    constraint ck_praxis_rule_audit_legal_hold_event_authorization check (
        authorization_reference_digest ~ '^[A-F0-9]{64}$')
);

create index if not exists idx_praxis_rule_audit_legal_hold_event_record
    on public.praxis_rule_audit_legal_hold_event (ledger_name, record_id, occurred_at);

revoke all on public.praxis_rule_audit_retention_guard from public;
revoke all on public.praxis_rule_audit_retention_run from public;
revoke all on public.praxis_rule_audit_legal_hold from public;
revoke all on public.praxis_rule_audit_legal_hold_event from public;

create or replace function public.reject_rule_audit_mutation()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
    if tg_op = 'DELETE' and exists (
        select 1
        from public.praxis_rule_audit_retention_guard guard
        where guard.transaction_id = txid_current()
          and guard.ledger_name = tg_table_name
    ) then
        return old;
    end if;
    raise exception '% is immutable; use the governed retention function', tg_table_name;
end;
$$;

drop trigger if exists trg_extraordinary_benefit_transformation_audit_append_only
    on public.extraordinary_benefit_transformation_audit;
create trigger trg_extraordinary_benefit_transformation_audit_append_only
before update or delete on public.extraordinary_benefit_transformation_audit
for each row execute function public.reject_rule_audit_mutation();

drop trigger if exists trg_extraordinary_benefit_statement_replay_audit_append_only
    on public.extraordinary_benefit_statement_replay_audit;
create trigger trg_extraordinary_benefit_statement_replay_audit_append_only
before update or delete on public.extraordinary_benefit_statement_replay_audit
for each row execute function public.reject_rule_audit_mutation();

create or replace function public.reject_rule_audit_control_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception '% is append-only', tg_table_name;
end;
$$;

drop trigger if exists trg_praxis_rule_audit_retention_run_append_only
    on public.praxis_rule_audit_retention_run;
create trigger trg_praxis_rule_audit_retention_run_append_only
before update or delete on public.praxis_rule_audit_retention_run
for each row execute function public.reject_rule_audit_control_mutation();

drop trigger if exists trg_praxis_rule_audit_legal_hold_event_append_only
    on public.praxis_rule_audit_legal_hold_event;
create trigger trg_praxis_rule_audit_legal_hold_event_append_only
before update or delete on public.praxis_rule_audit_legal_hold_event
for each row execute function public.reject_rule_audit_control_mutation();

create or replace function public.record_rule_audit_legal_hold(
    p_event_id uuid,
    p_ledger_name varchar,
    p_record_id uuid,
    p_hold_action varchar,
    p_reason_code varchar,
    p_authorization_reference_digest char(64))
returns uuid
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
    v_hold_id uuid;
    v_affected integer;
begin
    if p_event_id is null or p_record_id is null then
        raise exception 'event and record identifiers are required';
    end if;
    if p_ledger_name not in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit') then
        raise exception 'unsupported rule audit ledger';
    end if;
    if p_hold_action not in ('PLACE', 'RELEASE') then
        raise exception 'hold action must be PLACE or RELEASE';
    end if;
    if p_reason_code is null or btrim(p_reason_code) = '' or length(p_reason_code) > 120 then
        raise exception 'bounded reason code is required';
    end if;
    if p_authorization_reference_digest is null
            or p_authorization_reference_digest !~ '^[A-F0-9]{64}$' then
        raise exception 'authorization reference must be an uppercase HMAC-SHA-256 digest';
    end if;

    if p_hold_action = 'PLACE' then
        if p_ledger_name = 'extraordinary_benefit_statement_replay_audit' then
            perform 1
            from public.extraordinary_benefit_statement_replay_audit replay_audit
            where replay_audit.audit_id = p_record_id
            for update;
        else
            perform 1
            from public.extraordinary_benefit_transformation_audit transformation_audit
            where transformation_audit.audit_id = p_record_id
            for update;
        end if;
        get diagnostics v_affected = row_count;
        if v_affected <> 1 then
            raise exception 'rule audit record does not exist in the selected ledger';
        end if;

        v_hold_id := p_event_id;
        insert into public.praxis_rule_audit_legal_hold (
            ledger_name, record_id, hold_id, reason_code,
            authorization_reference_digest, placed_at)
        values (
            p_ledger_name, p_record_id, v_hold_id, p_reason_code,
            p_authorization_reference_digest, transaction_timestamp());
    else
        delete from public.praxis_rule_audit_legal_hold hold_state
        where hold_state.ledger_name = p_ledger_name
          and hold_state.record_id = p_record_id
        returning hold_state.hold_id into v_hold_id;
        get diagnostics v_affected = row_count;
        if v_affected <> 1 then
            raise exception 'no active legal hold exists for the record';
        end if;
    end if;

    insert into public.praxis_rule_audit_legal_hold_event (
        event_id, hold_id, ledger_name, record_id, hold_action, reason_code,
        authorization_reference_digest, occurred_at)
    values (
        p_event_id, v_hold_id, p_ledger_name, p_record_id, p_hold_action, p_reason_code,
        p_authorization_reference_digest, transaction_timestamp());
    return v_hold_id;
end;
$$;

create or replace function public.purge_rule_audit(
    p_retention_run_id uuid,
    p_ledger_name varchar,
    p_policy_key varchar,
    p_cutoff_utc timestamptz,
    p_batch_size integer,
    p_authorization_reference_digest char(64))
returns table (retention_run_id uuid, deleted_rows integer, batch_limit_reached boolean)
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
    v_deleted integer := 0;
begin
    if p_retention_run_id is null then
        raise exception 'retention run identifier is required';
    end if;
    if p_ledger_name not in (
        'extraordinary_benefit_statement_replay_audit',
        'extraordinary_benefit_transformation_audit') then
        raise exception 'unsupported rule audit ledger';
    end if;
    if p_policy_key is null or btrim(p_policy_key) = '' or length(p_policy_key) > 160 then
        raise exception 'bounded retention policy key is required';
    end if;
    if p_cutoff_utc is null or p_cutoff_utc > transaction_timestamp() - interval '24 hours' then
        raise exception 'cutoff must preserve at least the latest 24 hours';
    end if;
    if p_batch_size is null or p_batch_size < 1 or p_batch_size > 10000 then
        raise exception 'batch size must be between 1 and 10000';
    end if;
    if p_authorization_reference_digest is null
            or p_authorization_reference_digest !~ '^[A-F0-9]{64}$' then
        raise exception 'authorization reference must be an uppercase HMAC-SHA-256 digest';
    end if;

    insert into public.praxis_rule_audit_retention_guard (
        transaction_id, ledger_name, retention_run_id)
    values (txid_current(), p_ledger_name, p_retention_run_id);

    if p_ledger_name = 'extraordinary_benefit_statement_replay_audit' then
        with candidates as (
            select audit.audit_id
            from public.extraordinary_benefit_statement_replay_audit audit
            where audit.requested_at < p_cutoff_utc
              and not exists (
                  select 1
                  from public.praxis_rule_audit_legal_hold hold_state
                  where hold_state.ledger_name = p_ledger_name
                    and hold_state.record_id = audit.audit_id)
            order by audit.requested_at, audit.audit_id
            for update skip locked
            limit p_batch_size
        )
        delete from public.extraordinary_benefit_statement_replay_audit audit
        using candidates
        where audit.audit_id = candidates.audit_id;
    else
        with candidates as (
            select audit.audit_id
            from public.extraordinary_benefit_transformation_audit audit
            where audit.recorded_at < p_cutoff_utc
              and not exists (
                  select 1
                  from public.praxis_rule_audit_legal_hold hold_state
                  where hold_state.ledger_name = p_ledger_name
                    and hold_state.record_id = audit.audit_id)
            order by audit.recorded_at, audit.audit_id
            for update skip locked
            limit p_batch_size
        )
        delete from public.extraordinary_benefit_transformation_audit audit
        using candidates
        where audit.audit_id = candidates.audit_id;
    end if;
    get diagnostics v_deleted = row_count;

    delete from public.praxis_rule_audit_retention_guard guard
    where guard.transaction_id = txid_current()
      and guard.ledger_name = p_ledger_name;

    insert into public.praxis_rule_audit_retention_run (
        retention_run_id, ledger_name, policy_key, cutoff_utc, batch_size,
        deleted_rows, authorization_reference_digest, executed_at)
    values (
        p_retention_run_id, p_ledger_name, p_policy_key, p_cutoff_utc, p_batch_size,
        v_deleted, p_authorization_reference_digest, transaction_timestamp());

    return query select p_retention_run_id, v_deleted, v_deleted = p_batch_size;
exception
    when others then
        delete from public.praxis_rule_audit_retention_guard guard
        where guard.transaction_id = txid_current()
          and guard.ledger_name = p_ledger_name;
        raise;
end;
$$;

revoke all on function public.record_rule_audit_legal_hold(
    uuid, varchar, uuid, varchar, varchar, char(64)) from public;
revoke all on function public.purge_rule_audit(
    uuid, varchar, varchar, timestamptz, integer, char(64)) from public;

comment on function public.record_rule_audit_legal_hold(
    uuid, varchar, uuid, varchar, varchar, char(64)) is
    'Compliance-only legal hold boundary. Grant EXECUTE to a dedicated compliance role.';
comment on function public.purge_rule_audit(
    uuid, varchar, varchar, timestamptz, integer, char(64)) is
    'Retention-only bounded purge. Grant EXECUTE to a dedicated retention role.';
