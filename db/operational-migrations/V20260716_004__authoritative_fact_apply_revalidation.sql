-- FND-06: persist initial fact provenance and the exact pre-effect revalidation evidence.

alter table public.extraordinary_benefit_request
    add column if not exists fact_reference varchar(120),
    add column if not exists fact_provider_key varchar(160),
    add column if not exists fact_source_record_digest varchar(64),
    add column if not exists fact_source_version bigint,
    add column if not exists fact_source_recorded_at timestamptz,
    add column if not exists fact_scope_digest varchar(64),
    add column if not exists fact_as_of timestamptz;

alter table public.extraordinary_benefit_grant_effect
    add column if not exists revalidation_snapshot_key varchar(200),
    add column if not exists revalidation_snapshot_content_hash varchar(64),
    add column if not exists revalidation_facts_digest varchar(64),
    add column if not exists revalidation_provider_key varchar(160),
    add column if not exists revalidation_source_record_digest varchar(64),
    add column if not exists revalidation_source_version bigint,
    add column if not exists revalidation_source_recorded_at timestamptz,
    add column if not exists revalidated_at timestamptz,
    add column if not exists revalidation_scope_digest varchar(64);

alter table public.extraordinary_benefit_request
    drop constraint if exists ck_extraordinary_benefit_request_fact_source_digest,
    drop constraint if exists ck_extraordinary_benefit_request_fact_scope_digest;

alter table public.extraordinary_benefit_request
    add constraint ck_extraordinary_benefit_request_fact_source_digest
        check (fact_source_record_digest is null or fact_source_record_digest ~ '^[A-F0-9]{64}$'),
    add constraint ck_extraordinary_benefit_request_fact_scope_digest
        check (fact_scope_digest is null or fact_scope_digest ~ '^[A-F0-9]{64}$');

alter table public.extraordinary_benefit_grant_effect
    drop constraint if exists ck_extraordinary_benefit_effect_revalidation_snapshot_digest,
    drop constraint if exists ck_extraordinary_benefit_effect_revalidation_facts_digest,
    drop constraint if exists ck_extraordinary_benefit_effect_revalidation_source_digest,
    drop constraint if exists ck_extraordinary_benefit_effect_revalidation_scope_digest;

alter table public.extraordinary_benefit_grant_effect
    add constraint ck_extraordinary_benefit_effect_revalidation_snapshot_digest
        check (revalidation_snapshot_content_hash is null or revalidation_snapshot_content_hash ~ '^[A-F0-9]{64}$'),
    add constraint ck_extraordinary_benefit_effect_revalidation_facts_digest
        check (revalidation_facts_digest is null or revalidation_facts_digest ~ '^[A-F0-9]{64}$'),
    add constraint ck_extraordinary_benefit_effect_revalidation_source_digest
        check (revalidation_source_record_digest is null or revalidation_source_record_digest ~ '^[A-F0-9]{64}$'),
    add constraint ck_extraordinary_benefit_effect_revalidation_scope_digest
        check (revalidation_scope_digest is null or revalidation_scope_digest ~ '^[A-F0-9]{64}$');
