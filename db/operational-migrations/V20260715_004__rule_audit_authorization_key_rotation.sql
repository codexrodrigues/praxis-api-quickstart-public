-- P2F-ADR-12: make the external authorization HMAC auditable across key rotation.
-- The key id is an opaque version label, never a secret-manager path or secret value.

alter table public.praxis_rule_audit_retention_run
    add column if not exists authorization_key_id varchar(80);
alter table public.praxis_rule_audit_legal_hold
    add column if not exists authorization_key_id varchar(80);
alter table public.praxis_rule_audit_legal_hold_event
    add column if not exists authorization_key_id varchar(80);

-- Existing laboratory evidence predates key versioning. Preserve it explicitly instead of
-- inventing a key provenance that was not captured at execution time.
drop trigger if exists trg_praxis_rule_audit_retention_run_append_only
    on public.praxis_rule_audit_retention_run;
drop trigger if exists trg_praxis_rule_audit_legal_hold_event_append_only
    on public.praxis_rule_audit_legal_hold_event;

update public.praxis_rule_audit_retention_run
set authorization_key_id = 'LEGACY-UNVERSIONED'
where authorization_key_id is null;
update public.praxis_rule_audit_legal_hold
set authorization_key_id = 'LEGACY-UNVERSIONED'
where authorization_key_id is null;
update public.praxis_rule_audit_legal_hold_event
set authorization_key_id = 'LEGACY-UNVERSIONED'
where authorization_key_id is null;

alter table public.praxis_rule_audit_retention_run
    alter column authorization_key_id set not null;
alter table public.praxis_rule_audit_legal_hold
    alter column authorization_key_id set not null;
alter table public.praxis_rule_audit_legal_hold_event
    alter column authorization_key_id set not null;

create trigger trg_praxis_rule_audit_retention_run_append_only
before update or delete on public.praxis_rule_audit_retention_run
for each row execute function public.reject_rule_audit_control_mutation();
create trigger trg_praxis_rule_audit_legal_hold_event_append_only
before update or delete on public.praxis_rule_audit_legal_hold_event
for each row execute function public.reject_rule_audit_control_mutation();

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'ck_praxis_rule_audit_retention_run_authorization_key') then
        alter table public.praxis_rule_audit_retention_run
            add constraint ck_praxis_rule_audit_retention_run_authorization_key
            check (authorization_key_id ~ '^[A-Z0-9][A-Z0-9._:-]{2,79}$');
    end if;
    if not exists (
        select 1 from pg_constraint
        where conname = 'ck_praxis_rule_audit_legal_hold_authorization_key') then
        alter table public.praxis_rule_audit_legal_hold
            add constraint ck_praxis_rule_audit_legal_hold_authorization_key
            check (authorization_key_id ~ '^[A-Z0-9][A-Z0-9._:-]{2,79}$');
    end if;
    if not exists (
        select 1 from pg_constraint
        where conname = 'ck_praxis_rule_audit_legal_hold_event_authorization_key') then
        alter table public.praxis_rule_audit_legal_hold_event
            add constraint ck_praxis_rule_audit_legal_hold_event_authorization_key
            check (authorization_key_id ~ '^[A-Z0-9][A-Z0-9._:-]{2,79}$');
    end if;
end;
$$;

-- Remove the unversioned entry points so a caller cannot bypass key provenance after upgrade.
drop function if exists public.record_rule_audit_legal_hold(
    uuid, varchar, uuid, varchar, varchar, char(64));
drop function if exists public.purge_rule_audit(
    uuid, varchar, varchar, timestamptz, integer, char(64));

create or replace function public.record_rule_audit_legal_hold(
    p_event_id uuid,
    p_ledger_name varchar,
    p_record_id uuid,
    p_hold_action varchar,
    p_reason_code varchar,
    p_authorization_key_id varchar,
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
    if p_authorization_key_id is null
            or p_authorization_key_id !~ '^[A-Z0-9][A-Z0-9._:-]{2,79}$' then
        raise exception 'bounded authorization key id is required';
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
            ledger_name, record_id, hold_id, reason_code, authorization_key_id,
            authorization_reference_digest, placed_at)
        values (
            p_ledger_name, p_record_id, v_hold_id, p_reason_code, p_authorization_key_id,
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
        authorization_key_id, authorization_reference_digest, occurred_at)
    values (
        p_event_id, v_hold_id, p_ledger_name, p_record_id, p_hold_action, p_reason_code,
        p_authorization_key_id, p_authorization_reference_digest, transaction_timestamp());
    return v_hold_id;
end;
$$;

create or replace function public.purge_rule_audit(
    p_retention_run_id uuid,
    p_ledger_name varchar,
    p_policy_key varchar,
    p_cutoff_utc timestamptz,
    p_batch_size integer,
    p_authorization_key_id varchar,
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
    if p_authorization_key_id is null
            or p_authorization_key_id !~ '^[A-Z0-9][A-Z0-9._:-]{2,79}$' then
        raise exception 'bounded authorization key id is required';
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
        deleted_rows, authorization_key_id, authorization_reference_digest, executed_at)
    values (
        p_retention_run_id, p_ledger_name, p_policy_key, p_cutoff_utc, p_batch_size,
        v_deleted, p_authorization_key_id, p_authorization_reference_digest,
        transaction_timestamp());

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
    uuid, varchar, uuid, varchar, varchar, varchar, char(64)) from public;
revoke all on function public.purge_rule_audit(
    uuid, varchar, varchar, timestamptz, integer, varchar, char(64)) from public;

comment on function public.record_rule_audit_legal_hold(
    uuid, varchar, uuid, varchar, varchar, varchar, char(64)) is
    'Compliance-only legal hold boundary with versioned external HMAC provenance.';
comment on function public.purge_rule_audit(
    uuid, varchar, varchar, timestamptz, integer, varchar, char(64)) is
    'Retention-only bounded purge with versioned external HMAC provenance.';
