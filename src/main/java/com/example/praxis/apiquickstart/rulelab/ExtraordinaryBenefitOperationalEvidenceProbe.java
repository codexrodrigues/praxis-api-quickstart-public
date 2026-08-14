package com.example.praxis.apiquickstart.rulelab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Observa e remove somente fixtures descartaveis pertencentes a uma prova Policy Studio. */
@Component
class ExtraordinaryBenefitOperationalEvidenceProbe {
    static final String OWNED_REFERENCE_PREFIX = "policy-studio-proof-";
    private static final String EMPTY_DIGEST = digest(List.of());

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    ExtraordinaryBenefitOperationalEvidenceProbe(
            @Qualifier("apiJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("apiTransactionManager") PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    PolicyStudioOperationalEvidenceAdapter.OperationalState capture(String requestReference) {
        requireOwnedReference(requestReference);
        List<String> resource = jdbc.queryForList("""
                select concat_ws('|', reason_code, event_date, requested_amount, lifecycle_status,
                                  recommended_amount, currency, snapshot_content_hash, facts_digest,
                                  plan_digest, planned_effect_intent, effect_status, version)
                  from public.extraordinary_benefit_request
                 where request_reference = ?
                """, String.class, requestReference);
        List<String> audit = jdbc.queryForList("""
                select concat_ws('|', a.operation_cardinality, a.proposal_identity_digest,
                                  a.before_digest, a.after_digest, a.snapshot_content_hash,
                                  a.facts_digest, a.plan_digest)
                  from public.extraordinary_benefit_transformation_audit a
                  join public.extraordinary_benefit_request r on r.id = a.benefit_request_id
                 where r.request_reference = ?
                 order by a.recorded_at, a.audit_id
                """, String.class, requestReference);
        List<String> effects = jdbc.queryForList("""
                select concat_ws('|', e.intent_type, e.amount, e.currency,
                                  e.revalidation_snapshot_content_hash,
                                  e.revalidation_facts_digest, e.revalidation_scope_digest)
                  from public.extraordinary_benefit_grant_effect e
                  join public.extraordinary_benefit_request r on r.id = e.benefit_request_id
                 where r.request_reference = ?
                 order by e.effect_execution_id
                """, String.class, requestReference);
        return new PolicyStudioOperationalEvidenceAdapter.OperationalState(
                digest(List.of(digest(resource), digest(audit))),
                effects.isEmpty() ? EMPTY_DIGEST : digest(effects));
    }

    void cleanup(String requestReference) {
        requireOwnedReference(requestReference);
        transaction.executeWithoutResult(status -> {
            jdbc.update("""
                    delete from public.extraordinary_benefit_grant_effect
                     where benefit_request_id in (
                           select id from public.extraordinary_benefit_request where request_reference = ?)
                    """, requestReference);
            jdbc.update("""
                    delete from public.extraordinary_benefit_transformation_audit
                     where benefit_request_id in (
                           select id from public.extraordinary_benefit_request where request_reference = ?)
                    """, requestReference);
            jdbc.update("delete from public.extraordinary_benefit_request where request_reference = ?",
                    requestReference);
        });
    }

    private void requireOwnedReference(String requestReference) {
        if (requestReference == null || !requestReference.startsWith(OWNED_REFERENCE_PREFIX)
                || requestReference.length() == OWNED_REFERENCE_PREFIX.length()) {
            throw new IllegalArgumentException(
                    "Operational evidence may access only a uniquely owned Policy Studio proof reference");
        }
    }

    private static String digest(List<String> values) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            values.forEach(value -> {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                sha256.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                sha256.update((byte) ':');
                sha256.update(bytes);
            });
            return HexFormat.of().withUpperCase().formatHex(sha256.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }
}
