package com.example.praxis.apiquickstart.core.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/** Registro de idempotencia escopado por recurso, alvo, action e ator. */
@Entity
@Table(name = "praxis_resource_action_execution")
public class ResourceActionExecution {
    @Id @Column(name = "execution_id") private UUID executionId;
    @Column(name = "resource_key") private String resourceKey;
    @Column(name = "resource_id") private String resourceId;
    @Column(name = "action_id") private String actionId;
    @Column(name = "action_scope") private String actionScope;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "request_hash") private String requestHash;
    @Column(name = "execution_status") private String executionStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb") private JsonNode responsePayload;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "request_id") private String requestId;
    @Column(name = "actor_subject") private String actorSubject;
    @Column(name = "actor_authorities") private String actorAuthorities;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "failure_code") private String failureCode;
    @Column(name = "failure_message") private String failureMessage;
    public ResourceActionExecution() { }
    public ResourceActionExecution(UUID id, String resourceKey, String resourceId, String actionId, String actionScope, String idempotencyKey,
            String requestHash, String correlationId, String requestId, String actorSubject, String actorAuthorities) {
        this.executionId=id; this.resourceKey=resourceKey; this.resourceId=resourceId; this.actionId=actionId; this.actionScope=actionScope;
        this.idempotencyKey=idempotencyKey; this.requestHash=requestHash; this.correlationId=correlationId;
        this.requestId=requestId; this.actorSubject=actorSubject; this.actorAuthorities=actorAuthorities;
        this.executionStatus="STARTED"; this.startedAt=Instant.now();
    }
    public String getRequestHash() { return requestHash; }
    public UUID getExecutionId() { return executionId; }
    public String getExecutionStatus() { return executionStatus; }
    public JsonNode getResponsePayload() { return responsePayload; }
    public String getActorSubject() { return actorSubject; }
    public String getResourceId() { return resourceId; }
    public void complete(JsonNode payload) { this.executionStatus="COMPLETED"; this.responsePayload=payload; this.completedAt=Instant.now(); this.failureCode=null; this.failureMessage=null; }
    public void fail(String code, String message) {
        this.executionStatus = "FAILED";
        this.responsePayload = null;
        this.completedAt = Instant.now();
        this.failureCode = truncate(code, 120);
        this.failureMessage = truncate(message == null ? "Workflow execution failed." : message, 1000);
    }

    private String truncate(String value, int maximumLength) {
        return value == null || value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
