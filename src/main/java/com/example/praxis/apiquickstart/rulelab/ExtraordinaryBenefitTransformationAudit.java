package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Append-only, value-free evidence of one host-authorized transformation materialization. */
@Entity
@Immutable
@Table(name = "extraordinary_benefit_transformation_audit", schema = "public")
class ExtraordinaryBenefitTransformationAudit {
    @Id
    @Column(name = "audit_id", nullable = false)
    private UUID auditId;
    @Column(name = "benefit_request_id", nullable = false)
    private Long benefitRequestId;
    @Column(name = "operation_id")
    private UUID operationId;
    @Column(name = "operation_cardinality", nullable = false, length = 32)
    private String operationCardinality;
    @Column(name = "proposal_key", nullable = false, length = 200)
    private String proposalKey;
    @Column(name = "binding_key", nullable = false, length = 200)
    private String bindingKey;
    @Column(name = "slot_key", nullable = false, length = 200)
    private String slotKey;
    @Column(name = "target_path", nullable = false, length = 300)
    private String targetPath;
    @Column(name = "schema_ref", nullable = false, length = 500)
    private String schemaRef;
    @Column(name = "transformation_operation", nullable = false, length = 32)
    private String transformationOperation;
    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;
    @Column(name = "proposal_identity_digest", nullable = false, length = 64)
    private String proposalIdentityDigest;
    @Column(name = "before_digest", nullable = false, length = 64)
    private String beforeDigest;
    @Column(name = "after_digest", nullable = false, length = 64)
    private String afterDigest;
    @Column(name = "snapshot_key", nullable = false, length = 200)
    private String snapshotKey;
    @Column(name = "snapshot_content_hash", nullable = false, length = 64)
    private String snapshotContentHash;
    @Column(name = "snapshot_activation_revision", nullable = false)
    private long snapshotActivationRevision;
    @Column(name = "rule_set_key", nullable = false, length = 200)
    private String ruleSetKey;
    @Column(name = "rule_set_version", nullable = false)
    private int ruleSetVersion;
    @Column(name = "facts_digest", nullable = false, length = 64)
    private String factsDigest;
    @Column(name = "plan_digest", nullable = false, length = 64)
    private String planDigest;
    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ExtraordinaryBenefitTransformationAudit() {
    }

    ExtraordinaryBenefitTransformationAudit(
            UUID auditId,
            Long benefitRequestId,
            UUID operationId,
            RuleLabOperationCardinality cardinality,
            ExtraordinaryBenefitTransformationAuditEvidence evidence,
            com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse evaluation,
            String correlationId) {
        this.auditId = auditId;
        this.benefitRequestId = benefitRequestId;
        this.operationId = operationId;
        this.operationCardinality = cardinality.name();
        this.proposalKey = evidence.proposalKey();
        this.bindingKey = evidence.bindingKey();
        this.slotKey = evidence.slotKey();
        this.targetPath = evidence.targetPath();
        this.schemaRef = evidence.schemaRef();
        this.transformationOperation = evidence.operation();
        this.reasonCode = evidence.reasonCode();
        this.proposalIdentityDigest = evidence.proposalIdentityDigest();
        this.beforeDigest = evidence.beforeDigest();
        this.afterDigest = evidence.afterDigest();
        this.snapshotKey = evaluation.snapshotKey();
        this.snapshotContentHash = evaluation.snapshotContentHash();
        this.snapshotActivationRevision = evaluation.snapshotActivationRevision();
        this.ruleSetKey = evaluation.ruleSetKey();
        this.ruleSetVersion = evaluation.ruleSetVersion();
        this.factsDigest = evaluation.factsDigest();
        this.planDigest = evaluation.planDigest();
        this.correlationId = correlationId;
        this.recordedAt = evaluation.evaluatedAtUtc();
    }
}
