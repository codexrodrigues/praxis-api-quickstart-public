CREATE TABLE ui_user_config (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    etag UUID NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    environment VARCHAR(64),
    component_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    tags JSONB,
    tenant_id VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    user_id VARCHAR(255),
    UNIQUE (tenant_id, user_id, component_type, component_id, environment)
);

CREATE TABLE ai_thread (
    thread_id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    environment VARCHAR(64),
    user_id VARCHAR(128),
    component_type VARCHAR(64) NOT NULL,
    component_id VARCHAR(255) NOT NULL,
    route_key VARCHAR(255),
    title VARCHAR(120),
    status VARCHAR(16) NOT NULL,
    summary TEXT,
    schema_hash VARCHAR(128),
    variant_id VARCHAR(128),
    last_config_etag VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE ai_turn (
    thread_id UUID NOT NULL,
    turn_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    next_event_seq BIGINT NOT NULL DEFAULT 1,
    terminal_event_type VARCHAR(64),
    PRIMARY KEY (thread_id, turn_id),
    CONSTRAINT fk_ui_composition_turn_thread
        FOREIGN KEY (thread_id) REFERENCES ai_thread(thread_id)
);

CREATE TABLE ai_turn_event (
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    environment VARCHAR(64),
    stream_id UUID NOT NULL,
    thread_id UUID NOT NULL,
    turn_id UUID NOT NULL,
    seq BIGINT NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (thread_id, turn_id, seq),
    CONSTRAINT uk_ui_composition_event_id UNIQUE (event_id),
    CONSTRAINT fk_ui_composition_event_turn
        FOREIGN KEY (thread_id, turn_id) REFERENCES ai_turn(thread_id, turn_id)
);

CREATE INDEX idx_ui_composition_event_stream_seq
    ON ai_turn_event (stream_id, seq);
