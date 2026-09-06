-- A governed re-evaluation may materialize the same canonical proposal for new authoritative facts.
-- Command idempotency remains owned by praxis_resource_action_execution; this ledger stays append-only.
do $$
declare
    existing_constraint_type "char";
    existing_constraint_columns text[];
begin
    if to_regclass('public.extraordinary_benefit_transformation_audit') is not null then
        alter table public.extraordinary_benefit_transformation_audit
            drop constraint if exists uq_extraordinary_benefit_transformation_audit_proposal;

        select constraint_record.contype,
               array_agg(attribute_record.attname order by constraint_key.ordinality)
          into existing_constraint_type, existing_constraint_columns
          from pg_constraint constraint_record
          cross join lateral unnest(constraint_record.conkey)
              with ordinality as constraint_key(attnum, ordinality)
          join pg_attribute attribute_record
            on attribute_record.attrelid = constraint_record.conrelid
           and attribute_record.attnum = constraint_key.attnum
         where constraint_record.conrelid = 'public.extraordinary_benefit_transformation_audit'::regclass
           and constraint_record.conname = 'uq_extraordinary_benefit_transformation_audit_proposal_facts'
         group by constraint_record.contype;

        if existing_constraint_type is null then
            alter table public.extraordinary_benefit_transformation_audit
                add constraint uq_extraordinary_benefit_transformation_audit_proposal_facts
                    unique (benefit_request_id, proposal_identity_digest, facts_digest);
        elsif existing_constraint_type <> 'u'
           or existing_constraint_columns <> array[
               'benefit_request_id',
               'proposal_identity_digest',
               'facts_digest'
           ]::text[] then
            raise exception using
                message = 'Existing uq_extraordinary_benefit_transformation_audit_proposal_facts constraint is incompatible',
                detail = format(
                    'Expected UNIQUE (benefit_request_id, proposal_identity_digest, facts_digest), found type=%s columns=%s',
                    existing_constraint_type,
                    existing_constraint_columns
                );
        end if;
    end if;
end;
$$;
