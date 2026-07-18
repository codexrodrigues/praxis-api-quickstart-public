package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcExtraordinaryBenefitFactProviderTest {
    private static final String SCOPE_HMAC_KEY = "test-only-rule-fact-scope-key-32-bytes-minimum";
    private static final String SOURCE_DIGEST =
            "F8A520B6B03A57DE417F702EDE253622B794ADF72B31C814343887A3C629A995";
    private static final Instant AS_OF = Instant.parse("2026-07-16T12:00:00Z");

    private JdbcTemplate jdbc;
    private JdbcExtraordinaryBenefitFactProvider provider;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:authoritative_facts;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop table if exists public.rule_lab_authoritative_benefit_payment_date");
        jdbc.execute("drop table if exists public.rule_lab_authoritative_benefit_facts");
        createSchema(jdbc);
        provider = new JdbcExtraordinaryBenefitFactProvider(
                jdbc, readOnlyTransactions(dataSource), new RuleFactScopeDigester(SCOPE_HMAC_KEY));
    }

    @Test
    void loadsOneScopedTemporalVersionWithAllowlistedProvenance() {
        insertSnapshot("desenv", "local", "DEMO-ORG", "FACT-001", 1,
                Instant.parse("2026-01-01T00:00:00Z"), null, "ACTIVE", false);
        insertAllowedDate("desenv", "local", "DEMO-ORG", "FACT-001", 1, LocalDate.parse("2026-07-27"));
        insertAllowedDate("desenv", "local", "DEMO-ORG", "FACT-001", 1, LocalDate.parse("2026-07-20"));

        ExtraordinaryBenefitFactSnapshot snapshot = provider.load(
                new RuleFactLookup("desenv", "local", "DEMO-ORG", "FACT-001", AS_OF));

        assertThat(snapshot.facts().workerStatus()).isEqualTo("ACTIVE");
        assertThat(snapshot.facts().duplicateGrant()).isFalse();
        assertThat(snapshot.facts().allowedPaymentDates()).containsExactly(
                LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-27"));
        assertThat(snapshot.provenance().providerKey()).isEqualTo(JdbcExtraordinaryBenefitFactProvider.PROVIDER_KEY);
        assertThat(snapshot.provenance().sourceSystem()).isEqualTo("fictional-read-model");
        assertThat(snapshot.provenance().sourceRecordDigest()).isEqualTo(SOURCE_DIGEST);
        assertThat(snapshot.provenance().sourceVersion()).isEqualTo(1);
        assertThat(snapshot.provenance().asOf()).isEqualTo(AS_OF);
        assertThat(snapshot.provenance().scopeDigest())
                .matches("[0-9A-F]{64}")
                .doesNotContain("FACT-001", "DEMO-ORG", "desenv");
    }

    @Test
    void failsClosedWhenScopeDoesNotMatch() {
        insertSnapshot("tenant-a", "prod", "ORG-A", "FACT-001", 1,
                Instant.parse("2026-01-01T00:00:00Z"), null, "ACTIVE", false);

        assertThatThrownBy(() -> provider.load(
                new RuleFactLookup("tenant-b", "prod", "ORG-A", "FACT-001", AS_OF)))
                .isInstanceOfSatisfying(RuleFactUnavailableException.class,
                        failure -> assertThat(failure.code()).isEqualTo(RuleFactUnavailableException.NOT_FOUND))
                .hasMessage(RuleFactUnavailableException.NOT_FOUND);
    }

    @Test
    void failsClosedWhenTemporalVersionsOverlap() {
        insertSnapshot("desenv", "local", "DEMO-ORG", "FACT-001", 1,
                Instant.parse("2026-01-01T00:00:00Z"), null, "ACTIVE", false);
        insertSnapshot("desenv", "local", "DEMO-ORG", "FACT-001", 2,
                Instant.parse("2026-07-01T00:00:00Z"), null, "LEAVE", true);

        assertThatThrownBy(() -> provider.load(
                new RuleFactLookup("desenv", "local", "DEMO-ORG", "FACT-001", AS_OF)))
                .isInstanceOfSatisfying(RuleFactUnavailableException.class,
                        failure -> assertThat(failure.code()).isEqualTo(RuleFactUnavailableException.AMBIGUOUS))
                .hasMessage(RuleFactUnavailableException.AMBIGUOUS);
    }

    private TransactionTemplate readOnlyTransactions(DataSource dataSource) {
        TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactions.setReadOnly(true);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return transactions;
    }

    private void insertSnapshot(
            String tenant,
            String environment,
            String organization,
            String reference,
            long version,
            Instant effectiveFrom,
            Instant effectiveTo,
            String workerStatus,
            boolean duplicateGrant) {
        jdbc.update("""
                insert into public.rule_lab_authoritative_benefit_facts (
                    tenant_id, environment, organization_key, fact_reference,
                    source_system, source_record_digest, source_version,
                    effective_from, effective_to, worker_status, duplicate_grant,
                    program_active, program_maximum_amount, customer_additional_eligible,
                    available_budget_amount, recorded_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, 5000.00, true, 25000.00, ?)
                """,
                tenant, environment, organization, reference, "fictional-read-model", SOURCE_DIGEST, version,
                Timestamp.from(effectiveFrom), effectiveTo == null ? null : Timestamp.from(effectiveTo),
                workerStatus, duplicateGrant, Timestamp.from(Instant.parse("2026-07-16T00:00:00Z")));
    }

    private void insertAllowedDate(
            String tenant,
            String environment,
            String organization,
            String reference,
            long version,
            LocalDate date) {
        jdbc.update("""
                insert into public.rule_lab_authoritative_benefit_payment_date (
                    tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date
                ) values (?, ?, ?, ?, ?, ?)
                """, tenant, environment, organization, reference, version, date);
    }

    private void createSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table public.rule_lab_authoritative_benefit_facts (
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
                    program_maximum_amount numeric(15, 2) not null,
                    customer_additional_eligible boolean,
                    available_budget_amount numeric(15, 2) not null,
                    recorded_at timestamp with time zone not null,
                    primary key (tenant_id, environment, organization_key, fact_reference, source_version)
                )
                """);
        jdbcTemplate.execute("""
                create table public.rule_lab_authoritative_benefit_payment_date (
                    tenant_id varchar(120) not null,
                    environment varchar(80) not null,
                    organization_key varchar(120) not null,
                    fact_reference varchar(120) not null,
                    source_version bigint not null,
                    allowed_payment_date date not null,
                    primary key (tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date)
                )
                """);
    }
}
