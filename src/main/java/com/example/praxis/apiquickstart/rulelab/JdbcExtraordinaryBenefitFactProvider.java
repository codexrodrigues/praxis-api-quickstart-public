package com.example.praxis.apiquickstart.rulelab;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;

/** Le facts versionados do datasource operacional em uma transacao repetivel e sem mutacao. */
final class JdbcExtraordinaryBenefitFactProvider implements ExtraordinaryBenefitFactProvider {
    static final String PROVIDER_KEY = "quickstart:extraordinary-benefit-authoritative-facts";

    private static final String SNAPSHOT_SQL = """
            select source_system, source_record_digest, source_version, worker_status, duplicate_grant, program_active,
                   program_maximum_amount, customer_additional_eligible,
                   available_budget_amount, recorded_at
              from public.rule_lab_authoritative_benefit_facts
             where tenant_id = ?
               and environment = ?
               and organization_key = ?
               and fact_reference = ?
               and effective_from <= ?
               and (effective_to is null or effective_to > ?)
             order by source_version desc
             fetch first 3 rows only
            """;

    private static final String ALLOWED_DATES_SQL = """
            select allowed_payment_date
              from public.rule_lab_authoritative_benefit_payment_date
             where tenant_id = ?
               and environment = ?
               and organization_key = ?
               and fact_reference = ?
               and source_version = ?
             order by allowed_payment_date
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionOperations transactions;
    private final RuleFactScopeDigester scopeDigester;

    JdbcExtraordinaryBenefitFactProvider(
            JdbcTemplate jdbcTemplate,
            TransactionOperations transactions,
            RuleFactScopeDigester scopeDigester) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
        this.transactions = Objects.requireNonNull(transactions, "transactions is required");
        this.scopeDigester = Objects.requireNonNull(scopeDigester, "scopeDigester is required");
    }

    @Override
    public ExtraordinaryBenefitFactSnapshot load(RuleFactLookup lookup) {
        Objects.requireNonNull(lookup, "lookup is required");
        ExtraordinaryBenefitFactSnapshot snapshot = transactions.execute(status -> loadInTransaction(lookup));
        if (snapshot == null) {
            throw new RuleFactUnavailableException(RuleFactUnavailableException.NOT_FOUND);
        }
        return snapshot;
    }

    private ExtraordinaryBenefitFactSnapshot loadInTransaction(RuleFactLookup lookup) {
        Timestamp asOf = Timestamp.from(lookup.asOf());
        List<SnapshotRow> rows = jdbcTemplate.query(
                SNAPSHOT_SQL,
                (rs, rowNum) -> new SnapshotRow(
                        rs.getString("source_system"),
                        rs.getString("source_record_digest"),
                        rs.getLong("source_version"),
                        rs.getString("worker_status"),
                        rs.getBoolean("duplicate_grant"),
                        rs.getBoolean("program_active"),
                        rs.getBigDecimal("program_maximum_amount"),
                        (Boolean) rs.getObject("customer_additional_eligible"),
                        rs.getBigDecimal("available_budget_amount"),
                        rs.getTimestamp("recorded_at").toInstant()),
                lookup.tenantId(), lookup.environment(), lookup.organizationKey(), lookup.factReference(), asOf, asOf);
        if (rows.isEmpty()) {
            throw new RuleFactUnavailableException(RuleFactUnavailableException.NOT_FOUND);
        }
        if (rows.size() != 1) {
            throw new RuleFactUnavailableException(RuleFactUnavailableException.AMBIGUOUS);
        }

        SnapshotRow row = rows.getFirst();
        List<LocalDate> allowedDates = jdbcTemplate.query(
                ALLOWED_DATES_SQL,
                (rs, rowNum) -> rs.getObject("allowed_payment_date", LocalDate.class),
                lookup.tenantId(), lookup.environment(), lookup.organizationKey(), lookup.factReference(), row.sourceVersion());
        ExtraordinaryBenefitAuthoritativeFacts facts = new ExtraordinaryBenefitAuthoritativeFacts(
                row.workerStatus(), row.duplicateGrant(), row.programActive(), row.programMaximumAmount(),
                row.customerAdditionalEligible(), allowedDates, row.availableBudgetAmount());
        RuleFactProvenance provenance = new RuleFactProvenance(
                PROVIDER_KEY, row.sourceSystem(), row.sourceRecordDigest(), row.sourceVersion(),
                row.recordedAt(), lookup.asOf(), scopeDigester.digest(lookup));
        return new ExtraordinaryBenefitFactSnapshot(facts, provenance);
    }

    private record SnapshotRow(
            String sourceSystem,
            String sourceRecordDigest,
            long sourceVersion,
            String workerStatus,
            boolean duplicateGrant,
            boolean programActive,
            java.math.BigDecimal programMaximumAmount,
            Boolean customerAdditionalEligible,
            java.math.BigDecimal availableBudgetAmount,
            Instant recordedAt) {
    }
}
