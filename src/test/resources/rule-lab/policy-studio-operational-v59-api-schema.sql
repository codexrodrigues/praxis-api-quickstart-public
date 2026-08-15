CREATE TABLE extraordinary_benefit_request (
    id BIGSERIAL PRIMARY KEY,
    request_reference VARCHAR(80) NOT NULL UNIQUE,
    reason_code VARCHAR(40) NOT NULL,
    event_date DATE NOT NULL,
    requested_amount NUMERIC(15,2) NOT NULL,
    worker_status VARCHAR(20) NOT NULL,
    duplicate_grant BOOLEAN NOT NULL,
    program_active BOOLEAN NOT NULL,
    program_maximum_amount NUMERIC(15,2) NOT NULL,
    customer_additional_eligible BOOLEAN,
    requested_payment_date DATE NOT NULL,
    allowed_payment_dates VARCHAR(1000) NOT NULL,
    available_budget_amount NUMERIC(15,2) NOT NULL,
    user_time_zone VARCHAR(80) NOT NULL,
    lifecycle_status VARCHAR(20) NOT NULL,
    recommended_amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    snapshot_key VARCHAR(200) NOT NULL,
    snapshot_content_hash VARCHAR(64) NOT NULL,
    snapshot_activation_revision BIGINT NOT NULL,
    rule_set_key VARCHAR(200) NOT NULL,
    rule_set_version INTEGER NOT NULL,
    facts_digest VARCHAR(64) NOT NULL,
    fact_reference VARCHAR(120),
    fact_provider_key VARCHAR(160),
    fact_source_record_digest VARCHAR(64),
    fact_source_version BIGINT,
    fact_source_recorded_at TIMESTAMPTZ,
    fact_scope_digest VARCHAR(64),
    fact_as_of TIMESTAMPTZ,
    plan_digest VARCHAR(64) NOT NULL,
    planned_effect_intent VARCHAR(120) NOT NULL,
    evaluation_business_message VARCHAR(1000) NOT NULL,
    evaluation_reason_codes VARCHAR(1000) NOT NULL,
    effect_status VARCHAR(20) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    applied_at TIMESTAMPTZ,
    created_by VARCHAR(255) NOT NULL,
    last_transition_by VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE praxis_resource_action_execution (
    execution_id UUID PRIMARY KEY,
    resource_key VARCHAR(200) NOT NULL,
    resource_id VARCHAR(128) NOT NULL DEFAULT '__collection__',
    action_id VARCHAR(120) NOT NULL,
    action_scope VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    execution_status VARCHAR(32) NOT NULL,
    response_payload JSONB,
    correlation_id VARCHAR(255) NOT NULL,
    request_id VARCHAR(255),
    actor_subject VARCHAR(255) NOT NULL,
    actor_authorities TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(120),
    failure_message TEXT,
    CONSTRAINT uq_policy_v59_action_execution
        UNIQUE (resource_key, resource_id, action_id, actor_subject, idempotency_key)
);

CREATE TABLE extraordinary_benefit_transformation_audit (
    audit_id UUID PRIMARY KEY,
    benefit_request_id BIGINT NOT NULL REFERENCES extraordinary_benefit_request(id) ON DELETE RESTRICT,
    operation_id UUID,
    operation_cardinality VARCHAR(32) NOT NULL,
    proposal_key VARCHAR(200) NOT NULL,
    binding_key VARCHAR(200) NOT NULL,
    slot_key VARCHAR(200) NOT NULL,
    target_path VARCHAR(300) NOT NULL,
    schema_ref VARCHAR(500) NOT NULL,
    transformation_operation VARCHAR(32) NOT NULL,
    reason_code VARCHAR(120) NOT NULL,
    proposal_identity_digest VARCHAR(64) NOT NULL,
    before_digest VARCHAR(64) NOT NULL,
    after_digest VARCHAR(64) NOT NULL,
    snapshot_key VARCHAR(200) NOT NULL,
    snapshot_content_hash VARCHAR(64) NOT NULL,
    snapshot_activation_revision BIGINT NOT NULL,
    rule_set_key VARCHAR(200) NOT NULL,
    rule_set_version INTEGER NOT NULL,
    facts_digest VARCHAR(64) NOT NULL,
    plan_digest VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_policy_v59_transformation
        UNIQUE (benefit_request_id, proposal_identity_digest, facts_digest)
);

CREATE TABLE extraordinary_benefit_grant_effect (
    id BIGSERIAL PRIMARY KEY,
    effect_execution_id UUID NOT NULL UNIQUE,
    benefit_request_id BIGINT NOT NULL UNIQUE REFERENCES extraordinary_benefit_request(id) ON DELETE RESTRICT,
    request_reference VARCHAR(80) NOT NULL,
    intent_type VARCHAR(120) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL,
    executed_by VARCHAR(255) NOT NULL,
    revalidation_snapshot_key VARCHAR(200),
    revalidation_snapshot_content_hash VARCHAR(64),
    revalidation_facts_digest VARCHAR(64),
    revalidation_provider_key VARCHAR(160),
    revalidation_source_record_digest VARCHAR(64),
    revalidation_source_version BIGINT,
    revalidation_source_recorded_at TIMESTAMPTZ,
    revalidated_at TIMESTAMPTZ,
    revalidation_scope_digest VARCHAR(64)
);

CREATE TABLE rule_lab_authoritative_benefit_facts (
    tenant_id VARCHAR(120) NOT NULL,
    environment VARCHAR(80) NOT NULL,
    organization_key VARCHAR(120) NOT NULL,
    fact_reference VARCHAR(120) NOT NULL,
    source_system VARCHAR(120) NOT NULL,
    source_record_digest VARCHAR(64) NOT NULL,
    source_version BIGINT NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    worker_status VARCHAR(20) NOT NULL,
    duplicate_grant BOOLEAN NOT NULL,
    program_active BOOLEAN NOT NULL,
    program_maximum_amount NUMERIC(15,2) NOT NULL,
    customer_additional_eligible BOOLEAN,
    available_budget_amount NUMERIC(15,2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, environment, organization_key, fact_reference, source_version)
);

CREATE TABLE rule_lab_authoritative_benefit_payment_date (
    tenant_id VARCHAR(120) NOT NULL,
    environment VARCHAR(80) NOT NULL,
    organization_key VARCHAR(120) NOT NULL,
    fact_reference VARCHAR(120) NOT NULL,
    source_version BIGINT NOT NULL,
    allowed_payment_date DATE NOT NULL,
    PRIMARY KEY (tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date),
    FOREIGN KEY (tenant_id, environment, organization_key, fact_reference, source_version)
        REFERENCES rule_lab_authoritative_benefit_facts
        (tenant_id, environment, organization_key, fact_reference, source_version) ON DELETE CASCADE
);

INSERT INTO rule_lab_authoritative_benefit_facts (
    tenant_id, environment, organization_key, fact_reference, source_system,
    source_record_digest, source_version, effective_from, worker_status, duplicate_grant,
    program_active, program_maximum_amount, customer_additional_eligible,
    available_budget_amount, recorded_at)
VALUES (
    'desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001',
    'quickstart-fictional-hr-read-model',
    'F8A520B6B03A57DE417F702EDE253622B794ADF72B31C814343887A3C629A995', 1,
    TIMESTAMPTZ '2026-01-01 00:00:00+00', 'ACTIVE', FALSE, TRUE,
    5000.00, TRUE, 25000.00, TIMESTAMPTZ '2026-07-16 00:00:00+00');

INSERT INTO rule_lab_authoritative_benefit_payment_date (
    tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date)
VALUES
    ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001', 1, DATE '2026-07-20'),
    ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001', 1, DATE '2026-07-27');

CREATE TABLE rule_execution_observation_outbox (
    observation_id UUID PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    environment VARCHAR(128) NOT NULL,
    snapshot_key VARCHAR(128) NOT NULL,
    snapshot_content_hash VARCHAR(64) NOT NULL,
    activation_revision BIGINT NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    duration_micros BIGINT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    delivery_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_token UUID,
    lease_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    last_failure_code VARCHAR(120)
);
