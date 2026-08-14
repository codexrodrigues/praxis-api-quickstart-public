package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Keeps the disposable hosted runner aligned with the host's principal separation policy. */
class PayrollReactiveDeterminationHostedSmokeContractTest {

    @Test
    void runnerUsesIndependentGovernanceAndBusinessSessionsAndFailsClosed() throws Exception {
        String runner = Files.readString(Path.of(
                "scripts/workspace/Invoke-PayrollReactiveDeterminationHostedSmoke.sh"));

        assertThat(runner)
                .contains("BUSINESS_USERNAME_TENANT_A", "BUSINESS_USERNAME_TENANT_B")
                .contains("PUBLISHER_USERNAME_TENANT_A", "PUBLISHER_USERNAME_TENANT_B")
                .contains("Business and publisher sessions must be pairwise distinct across hosts.")
                .contains("publisher_status=$(call_net_salary")
                .contains("[[ \"$publisher_status\" == 403 ]]")
                .contains("fresh_status=$(call_net_salary")
                .contains("[[ \"$fresh_status\" == 200 ]]")
                .contains("lkg_status=$(call_net_salary")
                .contains("[[ \"$lkg_status\" == 200 ]]")
                .contains("recovery_status=$(call_net_salary")
                .contains("[[ \"$recovery_status\" == 200 ]]")
                .contains("trap restore_proxy EXIT INT TERM")
                .contains("sessionsDistinct:true")
                .doesNotContain("\"$proof_dir/publisher-$side.cookies\" \"$proof_dir/fresh-net");
    }
}
