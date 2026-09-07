CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR;

CREATE TABLE IF NOT EXISTS domain_rule_definition (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(128),
  environment VARCHAR(128),
  rule_key VARCHAR(512) NOT NULL,
  version INTEGER NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  context_key VARCHAR(255),
  resource_key VARCHAR(255),
  service_key VARCHAR(255),
  semantic_owner VARCHAR(255),
  steward VARCHAR(255),
  source_release_id UUID,
  source_change_set_id UUID,
  definition JSONB NOT NULL,
  parameters JSONB NOT NULL,
  condition JSONB,
  governance JSONB NOT NULL,
  validation_result JSONB,
  created_by_type VARCHAR(32) NOT NULL,
  created_by VARCHAR(255),
  approved_by VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  approved_at TIMESTAMP WITH TIME ZONE,
  activated_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_domain_rule_definition_scope_key_version
  ON domain_rule_definition (tenant_id, environment, rule_key, version);

CREATE TABLE IF NOT EXISTS domain_rule_materialization (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(128),
  environment VARCHAR(128),
  rule_definition_id UUID NOT NULL,
  materialization_key VARCHAR(512) NOT NULL,
  target_layer VARCHAR(64) NOT NULL,
  target_artifact_type VARCHAR(64) NOT NULL,
  target_artifact_key VARCHAR(768) NOT NULL,
  target_pointer VARCHAR(768),
  target_release_key VARCHAR(255),
  materialized_rule_id VARCHAR(512),
  status VARCHAR(32) NOT NULL,
  materialized_payload JSONB NOT NULL,
  source_hash VARCHAR(128),
  validation_result JSONB,
  applied_by_type VARCHAR(32),
  applied_by VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  applied_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_domain_rule_materialization_scope_key
  ON domain_rule_materialization (tenant_id, environment, materialization_key);

CREATE TABLE IF NOT EXISTS domain_rule_event (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(128),
  environment VARCHAR(128),
  rule_definition_id UUID NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  actor_type VARCHAR(32),
  actor VARCHAR(255),
  summary VARCHAR(512) NOT NULL,
  status VARCHAR(32),
  target_layer VARCHAR(64),
  target_artifact_type VARCHAR(64),
  target_artifact_key VARCHAR(768),
  materialization_id UUID,
  materialization_key VARCHAR(512),
  source_hash VARCHAR(128),
  visibility VARCHAR(32) NOT NULL,
  safe_metadata JSONB NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS domain_rule_definition_approval (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(128) NOT NULL,
  environment VARCHAR(128) NOT NULL,
  definition_id UUID NOT NULL,
  definition_hash VARCHAR(64) NOT NULL,
  actor_ref VARCHAR(255) NOT NULL,
  role VARCHAR(64) NOT NULL,
  approved_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uq_domain_rule_definition_approval_actor
    UNIQUE (tenant_id, environment, definition_id, definition_hash, actor_ref)
);

CREATE TABLE IF NOT EXISTS domain_rule_change_workspace (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(128) NOT NULL,
  environment VARCHAR(128) NOT NULL,
  rule_key VARCHAR(512) NOT NULL,
  base_definition_id UUID NOT NULL,
  base_definition_version INTEGER NOT NULL,
  base_definition_hash VARCHAR(64) NOT NULL,
  promoted_definition_id UUID,
  submitted_test_run_id UUID,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  draft_condition JSONB,
  draft_parameters JSONB NOT NULL,
  rationale TEXT,
  etag UUID NOT NULL,
  revision BIGINT NOT NULL,
  created_by VARCHAR(255) NOT NULL,
  updated_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  row_version BIGINT NOT NULL
);
