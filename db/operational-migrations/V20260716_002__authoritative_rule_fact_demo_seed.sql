-- Fictional, sanitized FND-06 fixture. It proves acquisition mechanics only.
-- It must never be described as an Ergon baseline, shadow result or production authority.
-- source_record_digest = SHA-256 (UTF-8, uppercase) of:
-- quickstart-fictional-hr-read-model|QL10-FICTIONAL-001|1|ACTIVE|false|true|5000.00|true|25000.00|2026-07-20,2026-07-27

insert into public.rule_lab_authoritative_benefit_facts (
    tenant_id, environment, organization_key, fact_reference,
    source_system, source_record_digest, source_version,
    effective_from, effective_to, worker_status, duplicate_grant,
    program_active, program_maximum_amount, customer_additional_eligible,
    available_budget_amount, recorded_at
) values (
    'desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001',
    'quickstart-fictional-hr-read-model',
    'F8A520B6B03A57DE417F702EDE253622B794ADF72B31C814343887A3C629A995', 1,
    timestamp with time zone '2026-01-01 00:00:00+00', null,
    'ACTIVE', false, true, 5000.00, true, 25000.00,
    timestamp with time zone '2026-07-16 00:00:00+00'
) on conflict do nothing;

insert into public.rule_lab_authoritative_benefit_payment_date (
    tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date
) values
    ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001', 1, date '2026-07-20'),
    ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001', 1, date '2026-07-27')
on conflict do nothing;
