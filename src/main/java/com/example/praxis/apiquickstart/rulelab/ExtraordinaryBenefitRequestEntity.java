package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@Getter
@Setter
@Entity
@Table(name = "extraordinary_benefit_request", schema = "public")
public class ExtraordinaryBenefitRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_reference", nullable = false, length = 80, unique = true)
    private String requestReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private ExtraordinaryBenefitReason reasonCode;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "worker_status", nullable = false, length = 20)
    private String workerStatus;

    @Column(name = "duplicate_grant", nullable = false)
    private boolean duplicateGrant;

    @Column(name = "program_active", nullable = false)
    private boolean programActive;

    @Column(name = "program_maximum_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal programMaximumAmount;

    @Column(name = "customer_additional_eligible")
    private Boolean customerAdditionalEligible;

    @Column(name = "requested_payment_date", nullable = false)
    private LocalDate requestedPaymentDate;

    @Column(name = "allowed_payment_dates", nullable = false, length = 1000)
    private String allowedPaymentDates;

    @Column(name = "available_budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBudgetAmount;

    @Column(name = "user_time_zone", nullable = false, length = 80)
    private String userTimeZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private ExtraordinaryBenefitLifecycleStatus lifecycleStatus;

    @Column(name = "recommended_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal recommendedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

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

    @Column(name = "fact_reference", length = 120)
    private String factReference;

    @Column(name = "fact_provider_key", length = 160)
    private String factProviderKey;

    @Column(name = "fact_source_record_digest", length = 64)
    private String factSourceRecordDigest;

    @Column(name = "fact_source_version")
    private Long factSourceVersion;

    @Column(name = "fact_source_recorded_at")
    private Instant factSourceRecordedAt;

    @Column(name = "fact_scope_digest", length = 64)
    private String factScopeDigest;

    @Column(name = "fact_as_of")
    private Instant factAsOf;

    @Column(name = "plan_digest", nullable = false, length = 64)
    private String planDigest;

    @Column(name = "planned_effect_intent", nullable = false, length = 120)
    private String plannedEffectIntent;

    @Column(name = "evaluation_business_message", nullable = false, length = 1000)
    private String evaluationBusinessMessage;

    @Column(name = "evaluation_reason_codes", nullable = false, length = 1000)
    private String evaluationReasonCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_status", nullable = false, length = 20)
    private ExtraordinaryBenefitEffectStatus effectStatus;

    @DefaultSortColumn(priority = 1, ascending = false)
    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "last_transition_by", nullable = false, length = 255)
    private String lastTransitionBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OptionLabel
    public String getLabel() {
        return requestReference + " - " + lifecycleStatus;
    }
}
