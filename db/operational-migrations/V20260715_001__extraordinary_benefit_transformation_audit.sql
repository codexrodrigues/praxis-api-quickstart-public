-- P2F-ADR-11: redacted, append-only evidence for host-authorized transformations.
-- No request reference, before value, after value or serialized facts are persisted here.

create table if not exists public.extraordinary_benefit_transformation_audit (
    audit_id uuid primary key,
    benefit_request_id bigint not null,
    operation_id uuid,
    operation_cardinality varchar(32) not null,
    proposal_key varchar(200) not null,
    binding_key varchar(200) not null,
    slot_key varchar(200) not null,
    target_path varchar(300) not null,
    schema_ref varchar(500) not null,
    transformation_operation varchar(32) not null,
    reason_code varchar(120) not null,
    proposal_identity_digest varchar(64) not null,
    before_digest varchar(64) not null,
    after_digest varchar(64) not null,
    snapshot_key varchar(200) not null,
    snapshot_content_hash varchar(64) not null,
    snapshot_activation_revision bigint not null,
    rule_set_key varchar(200) not null,
    rule_set_version integer not null,
    facts_digest varchar(64) not null,
    plan_digest varchar(64) not null,
    correlation_id varchar(255) not null,
    recorded_at timestamptz not null,
    constraint fk_extraordinary_benefit_transformation_audit_request
        foreign key (benefit_request_id)
        references public.extraordinary_benefit_request (id)
        on delete restrict,
    constraint uq_extraordinary_benefit_transformation_audit_proposal
        unique (benefit_request_id, proposal_identity_digest),
    constraint ck_extraordinary_benefit_transformation_audit_cardinality
        check (operation_cardinality in ('SINGLE_ITEM', 'ITEM_INDEPENDENT', 'STATEMENT_ATOMIC')),
    constraint ck_extraordinary_benefit_transformation_audit_operation
        check (transformation_operation in ('SET', 'REMOVE')),
    constraint ck_extraordinary_benefit_transformation_audit_digests
        check (proposal_identity_digest ~ '^[A-F0-9]{64}$'
            and before_digest ~ '^[A-F0-9]{64}$'
            and after_digest ~ '^[A-F0-9]{64}$'
            and snapshot_content_hash ~ '^[A-F0-9]{64}$'
            and facts_digest ~ '^[A-F0-9]{64}$'
            and plan_digest ~ '^[A-F0-9]{64}$')
);

create index if not exists idx_extraordinary_benefit_transformation_audit_request
    on public.extraordinary_benefit_transformation_audit (benefit_request_id, recorded_at);

create index if not exists idx_extraordinary_benefit_transformation_audit_correlation
    on public.extraordinary_benefit_transformation_audit (correlation_id, recorded_at);

create or replace function public.reject_extraordinary_benefit_transformation_audit_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'extraordinary_benefit_transformation_audit is append-only';
end;
$$;

drop trigger if exists trg_extraordinary_benefit_transformation_audit_append_only
    on public.extraordinary_benefit_transformation_audit;
create trigger trg_extraordinary_benefit_transformation_audit_append_only
before update or delete on public.extraordinary_benefit_transformation_audit
for each row execute function public.reject_extraordinary_benefit_transformation_audit_mutation();
