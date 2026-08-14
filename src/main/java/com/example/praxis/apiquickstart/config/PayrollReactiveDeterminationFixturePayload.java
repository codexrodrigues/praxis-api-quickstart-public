package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.praxisplatform.config.dto.DomainRuleSnapshotPublicationRequest;

/**
 * Serializes the disposable hosted fixture from the host-owned canonical payroll RuleSet.
 *
 * <p>This is a deployment-proof adapter, not another decision definition. Keeping it in the same
 * package lets the fixture consume {@link PayrollReactiveDeterminationRuleSet} directly.</p>
 */
public final class PayrollReactiveDeterminationFixturePayload {
    private PayrollReactiveDeterminationFixturePayload() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("expected netDefinitionId paymentDefinitionId version");
        }
        System.out.println(serialize(args[0], args[1], Integer.parseInt(args[2]), Instant.now()));
    }

    static String serialize(String netDefinitionId, String paymentDefinitionId, int version, Instant now)
            throws Exception {
        Instant effectiveNow = now.truncatedTo(ChronoUnit.SECONDS);
        var request = new DomainRuleSnapshotPublicationRequest(
                PayrollReactiveDeterminationRuleSet.definition(version),
                List.of(UUID.fromString(netDefinitionId), UUID.fromString(paymentDefinitionId)),
                PayrollReactiveDeterminationRuleSet.OWNER_SERVICE_KEY,
                PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION,
                effectiveNow.minus(1, ChronoUnit.HOURS).toString(),
                effectiveNow.plus(1, ChronoUnit.DAYS).toString(),
                null);
        return new ObjectMapper().findAndRegisterModules().writeValueAsString(request);
    }
}
