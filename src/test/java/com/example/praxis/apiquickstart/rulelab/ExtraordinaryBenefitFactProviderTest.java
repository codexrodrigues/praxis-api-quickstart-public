package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class ExtraordinaryBenefitFactProviderTest {
    private static final String DIGEST = "A".repeat(64);
    private static final Instant AS_OF = Instant.parse("2026-07-15T20:00:00Z");

    private JdbcTemplate jdbc;
    private ExtraordinaryBenefitFactProvider provider;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                create table rule_lab_authoritative_benefit_facts (
                    tenant_id varchar(120) not null,
                    environment varchar(80) not null,
                    organization_key varchar(120) not null,
                    fact_reference varchar(120) not null,
                    source_system varchar(120) not null,
                    source_record_digest varchar(64) not null,
                    source_version bigint not null,
                    effective_from timestamp with time zone not null,
                    effective_to timestamp with time zone,
                    worker_status varchar(20) not null,
                    duplicate_grant boolean not null,
                    program_active boolean not null,
                    program_maximum_amount numeric(15,2) not null,
                    customer_additional_eligible boolean,
                    available_budget_amount numeric(15,2) not null,
                    recorded_at timestamp with time zone not null,
                    primary key (tenant_id, environment, organization_key, fact_reference, source_version)
                )
                """);
        jdbc.execute("""
                create table rule_lab_authoritative_benefit_payment_date (
                    tenant_id varchar(120) not null,
                    environment varchar(80) not null,
                    organization_key varchar(120) not null,
                    fact_reference varchar(120) not null,
                    source_version bigint not null,
                    allowed_payment_date date not null
                )
                """);
        insertFact("tenant-a", "prod", "ORG-1", "FACT-1", 3, "ACTIVE", false, "9000.00");
        jdbc.update("""
                insert into rule_lab_authoritative_benefit_payment_date
                    (tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date)
                values (?, ?, ?, ?, ?, ?)
                """, "tenant-a", "prod", "ORG-1", "FACT-1", 3, LocalDate.parse("2026-07-20"));

        var transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactions.setReadOnly(true);
        provider = new JdbcExtraordinaryBenefitFactProvider(
                jdbc, transactions, new RuleFactScopeDigester("test-secret-key-with-at-least-32-bytes"));
    }

    @Test
    void loadsVersionedFactsAndSanitizedProvenanceFromTheExactHostScope() {
        ExtraordinaryBenefitFactSnapshot snapshot =
                provider.load(new RuleFactLookup("tenant-a", "prod", "ORG-1", "FACT-1", AS_OF));

        assertThat(snapshot.facts().workerStatus()).isEqualTo("ACTIVE");
        assertThat(snapshot.facts().duplicateGrant()).isFalse();
        assertThat(snapshot.facts().programMaximumAmount()).isEqualByComparingTo("5000.00");
        assertThat(snapshot.facts().availableBudgetAmount()).isEqualByComparingTo("9000.00");
        assertThat(snapshot.facts().allowedPaymentDates()).containsExactly(LocalDate.parse("2026-07-20"));
        assertThat(snapshot.provenance().providerKey())
                .isEqualTo(JdbcExtraordinaryBenefitFactProvider.PROVIDER_KEY);
        assertThat(snapshot.provenance().sourceSystem()).isEqualTo("governed-hr-read-model");
        assertThat(snapshot.provenance().sourceRecordDigest()).isEqualTo(DIGEST);
        assertThat(snapshot.provenance().sourceVersion()).isEqualTo(3);
        assertThat(snapshot.provenance().asOf()).isEqualTo(AS_OF);
        assertThat(snapshot.provenance().scopeDigest()).matches("[A-F0-9]{64}");
    }

    @Test
    void failsClosedWhenTheExactScopeHasNoSnapshot() {
        assertThatThrownBy(() ->
                provider.load(new RuleFactLookup("other-tenant", "prod", "ORG-1", "FACT-1", AS_OF)))
                .isInstanceOfSatisfying(RuleFactUnavailableException.class,
                        failure -> assertThat(failure.code()).isEqualTo(RuleFactUnavailableException.NOT_FOUND));
    }

    @Test
    void failsClosedInsteadOfSelectingOneOfOverlappingEffectiveVersions() {
        insertFact("tenant-a", "prod", "ORG-1", "FACT-1", 4, "LEAVE", true, "8000.00");

        assertThatThrownBy(() ->
                provider.load(new RuleFactLookup("tenant-a", "prod", "ORG-1", "FACT-1", AS_OF)))
                .isInstanceOfSatisfying(RuleFactUnavailableException.class,
                        failure -> assertThat(failure.code()).isEqualTo(RuleFactUnavailableException.AMBIGUOUS));
    }

    @Test
    void scopeDigestChangesWithoutPublishingTheScopeInClearText() {
        ExtraordinaryBenefitFactSnapshot first =
                provider.load(new RuleFactLookup("tenant-a", "prod", "ORG-1", "FACT-1", AS_OF));
        insertFact("tenant-a", "prod", "ORG-1", "FACT-2", 1, "ACTIVE", false, "9000.00");
        ExtraordinaryBenefitFactSnapshot second =
                provider.load(new RuleFactLookup("tenant-a", "prod", "ORG-1", "FACT-2", AS_OF));

        assertThat(second.provenance().scopeDigest()).isNotEqualTo(first.provenance().scopeDigest());
        assertThat(second.provenance().scopeDigest())
                .doesNotContain("tenant-a")
                .doesNotContain("ORG-1")
                .doesNotContain("FACT-2");
    }

    private void insertFact(
            String tenant,
            String environment,
            String organization,
            String reference,
            long version,
            String workerStatus,
            boolean duplicateGrant,
            String budget) {
        jdbc.update("""
                insert into rule_lab_authoritative_benefit_facts (
                    tenant_id, environment, organization_key, fact_reference,
                    source_system, source_record_digest, source_version,
                    effective_from, effective_to, worker_status, duplicate_grant,
                    program_active, program_maximum_amount, customer_additional_eligible,
                    available_budget_amount, recorded_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, null, ?, ?, true, 5000.00, true, ?, ?)
                """,
                tenant,
                environment,
                organization,
                reference,
                "governed-hr-read-model",
                DIGEST,
                version,
                Instant.parse("2026-01-01T00:00:00Z"),
                workerStatus,
                duplicateGrant,
                budget,
                Instant.parse("2026-07-01T00:00:00Z"));
    }
}
