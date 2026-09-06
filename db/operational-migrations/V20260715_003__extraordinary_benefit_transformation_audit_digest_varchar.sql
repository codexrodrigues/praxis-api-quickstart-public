-- Align legacy ADR-11 evidence columns with the JPA String contract.
-- rtrim preserves the digest value when a prior manual migration created CHAR(64).

alter table public.extraordinary_benefit_transformation_audit
    alter column proposal_identity_digest type varchar(64) using rtrim(proposal_identity_digest),
    alter column before_digest type varchar(64) using rtrim(before_digest),
    alter column after_digest type varchar(64) using rtrim(after_digest),
    alter column snapshot_content_hash type varchar(64) using rtrim(snapshot_content_hash),
    alter column facts_digest type varchar(64) using rtrim(facts_digest),
    alter column plan_digest type varchar(64) using rtrim(plan_digest);
