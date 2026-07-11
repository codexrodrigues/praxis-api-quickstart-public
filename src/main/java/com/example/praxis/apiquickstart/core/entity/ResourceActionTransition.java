package com.example.praxis.apiquickstart.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Append-only audit record for a confirmed resource workflow transition. */
@Entity
@Table(name = "praxis_resource_action_transition")
public class ResourceActionTransition {
    @Id @Column(name = "transition_id", nullable = false) private UUID transitionId;
    @Column(name = "resource_key", nullable = false) private String resourceKey;
    @Column(name = "resource_id", nullable = false) private String resourceId;
    @Column(name = "action_id", nullable = false) private String actionId;
    @Column(name = "action_scope", nullable = false) private String actionScope;
    @Column(name = "previous_state") private String previousState;
    @Column(name = "resulting_state") private String resultingState;
    @Column(name = "reason_code") private String reasonCode;
    @Column(name = "comment") private String comment;
    @Column(name = "effective_at", nullable = false) private LocalDate effectiveAt;
    @Column(name = "performed_at", nullable = false) private Instant performedAt;
    @Column(name = "actor_subject", nullable = false) private String actorSubject;
    @Column(name = "actor_authorities") private String actorAuthorities;
    @Column(name = "correlation_id", nullable = false) private String correlationId;
    @Column(name = "request_id") private String requestId;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "version_before") private Long versionBefore;
    @Column(name = "version_after") private Long versionAfter;

    public ResourceActionTransition() { }
    public ResourceActionTransition(UUID id, String key, String resourceId, String actionId, String actionScope, String previousState, String resultingState, String reasonCode, String comment, LocalDate effectiveAt, Instant performedAt, String actorSubject, String actorAuthorities, String correlationId, String requestId, String idempotencyKey, Long before, Long after) {
        this.transitionId=id; this.resourceKey=key; this.resourceId=resourceId; this.actionId=actionId; this.actionScope=actionScope; this.previousState=previousState; this.resultingState=resultingState; this.reasonCode=reasonCode; this.comment=comment; this.effectiveAt=effectiveAt; this.performedAt=performedAt; this.actorSubject=actorSubject; this.actorAuthorities=actorAuthorities; this.correlationId=correlationId; this.requestId=requestId; this.idempotencyKey=idempotencyKey; this.versionBefore=before; this.versionAfter=after;
    }
    public UUID getTransitionId() { return transitionId; }
}
