package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** Ledger do efeito realmente executado pelo host, separado da intencao pura do engine. */
@Getter
@Entity
@Table(name = "extraordinary_benefit_grant_effect", schema = "public")
public class ExtraordinaryBenefitGrantEffect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "effect_execution_id", nullable = false, unique = true)
    private UUID effectExecutionId;

    @Column(name = "benefit_request_id", nullable = false, unique = true)
    private Long benefitRequestId;

    @Column(name = "request_reference", nullable = false, length = 80)
    private String requestReference;

    @Column(name = "intent_type", nullable = false, length = 120)
    private String intentType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "executed_by", nullable = false, length = 255)
    private String executedBy;

    @Column(name = "revalidation_snapshot_key", nullable = false, length = 200)
    private String revalidationSnapshotKey;

    @Column(name = "revalidation_snapshot_content_hash", nullable = false, length = 64)
    private String revalidationSnapshotContentHash;

    @Column(name = "revalidation_facts_digest", nullable = false, length = 64)
    private String revalidationFactsDigest;

    @Column(name = "revalidation_provider_key", nullable = false, length = 160)
    private String revalidationProviderKey;

    @Column(name = "revalidation_source_record_digest", nullable = false, length = 64)
    private String revalidationSourceRecordDigest;

    @Column(name = "revalidation_source_version", nullable = false)
    private long revalidationSourceVersion;

    @Column(name = "revalidation_source_recorded_at", nullable = false)
    private Instant revalidationSourceRecordedAt;

    @Column(name = "revalidated_at", nullable = false)
    private Instant revalidatedAt;

    @Column(name = "revalidation_scope_digest", nullable = false, length = 64)
    private String revalidationScopeDigest;

    protected ExtraordinaryBenefitGrantEffect() {
    }

    public ExtraordinaryBenefitGrantEffect(
            UUID effectExecutionId,
            Long benefitRequestId,
            String requestReference,
            String intentType,
            BigDecimal amount,
            String currency,
            Instant executedAt,
            String executedBy,
            String revalidationSnapshotKey,
            String revalidationSnapshotContentHash,
            String revalidationFactsDigest,
            String revalidationProviderKey,
            String revalidationSourceRecordDigest,
            long revalidationSourceVersion,
            Instant revalidationSourceRecordedAt,
            Instant revalidatedAt,
            String revalidationScopeDigest) {
        this.effectExecutionId = effectExecutionId;
        this.benefitRequestId = benefitRequestId;
        this.requestReference = requestReference;
        this.intentType = intentType;
        this.amount = amount;
        this.currency = currency;
        this.executedAt = executedAt;
        this.executedBy = executedBy;
        this.revalidationSnapshotKey = revalidationSnapshotKey;
        this.revalidationSnapshotContentHash = revalidationSnapshotContentHash;
        this.revalidationFactsDigest = revalidationFactsDigest;
        this.revalidationProviderKey = revalidationProviderKey;
        this.revalidationSourceRecordDigest = revalidationSourceRecordDigest;
        this.revalidationSourceVersion = revalidationSourceVersion;
        this.revalidationSourceRecordedAt = revalidationSourceRecordedAt;
        this.revalidatedAt = revalidatedAt;
        this.revalidationScopeDigest = revalidationScopeDigest;
    }
}
