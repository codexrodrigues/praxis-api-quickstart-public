package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Locks the fail-closed orchestration contract without creating remote resources. */
class PayrollReactiveDeterminationHostedBootstrapContractTest {

    private static final Path LIFECYCLE = Path.of(
            "scripts/workspace/Invoke-PayrollReactiveDeterminationHostedProof.sh");
    private static final Path FIXTURE = Path.of(
            "scripts/workspace/Provision-PayrollReactiveDeterminationHostedFixture.py");

    @Test
    void lifecyclePinsSourceUsesVersionedBootstrapPollsTerminalStatesAndAlwaysCleansUp() throws Exception {
        String script = Files.readString(LIFECYCLE);

        assertThat(script)
                .contains("HOSTED_PROOF_CONFIRM")
                .contains("CREATE_DISPOSABLE_RENDER_RESOURCES")
                .contains("[[ -z \"$(git status --porcelain)\" ]]")
                .contains("git push origin \"$SOURCE_COMMIT:refs/heads/$branch\"")
                .contains("cleanup-before-create")
                .contains("services-after-precleanup.json", "postgres-after-precleanup.json")
                .contains("poll_postgres", "poll_service")
                .contains("build_failed|update_failed|canceled|deactivated")
                .contains("HOSTED_DEPLOY_TIMEOUT_SECONDS", "HOSTED_TTL_SECONDS")
                .contains("trap cleanup EXIT INT TERM")
                .contains("git push origin --delete \"$branch\"")
                .contains("rm -f \"$secrets\"")
                .contains("SPRING_JPA_HIBERNATE_DDL_AUTO=validate")
                .contains("PRAXIS_OPERATIONAL_BOOTSTRAP_MODE=hosted-public-demo-fixture")
                .doesNotContain("SPRING_JPA_HIBERNATE_DDL_AUTO=update");
    }

    @Test
    void fixtureIsIdempotentAndConsumesTheCanonicalJavaAggregate() throws Exception {
        String script = Files.readString(FIXTURE);

        assertThat(script)
                .contains("PayrollReactiveDeterminationFixturePayload")
                .contains("VERIFIED_EXISTING", "RESUMED", "PROVISIONED")
                .contains("versions != [1, 2]")
                .contains("snapshot.get(\"ruleSet\") != expected.get(\"ruleSet\")")
                .contains("Published payroll head escaped the requested tenant/environment scope")
                .contains("Author, publisher and both composition approvers must be distinct")
                .contains("clients[\"author\"].request")
                .doesNotContain("PayrollReactiveDeterminationRuleSet =")
                .doesNotContain("print(password)", "print(IDENTITIES)");
    }

    @Test
    void invalidExplicitConfirmationStopsBeforeAnyProviderCall(@TempDir Path tempDir) throws Exception {
        Path marker = tempDir.resolve("render-was-called");
        Path fakeBin = tempDir.resolve("bin");
        Files.createDirectory(fakeBin);
        Path render = fakeBin.resolve("render");
        Files.writeString(render, "#!/bin/sh\nprintf called > '" + marker + "'\nexit 99\n");
        render.toFile().setExecutable(true);

        var environment = new HashMap<>(System.getenv());
        environment.put("PATH", fakeBin + ":" + environment.get("PATH"));
        environment.put("SMOKE_RUN_ID", "20260813T120000Z");
        environment.put("SOURCE_COMMIT", "a".repeat(40));
        environment.put("RENDER_WORKSPACE_ID", "tea-fixture");
        environment.put("HOSTED_PROOF_CONFIRM", "NO");
        ProcessBuilder builder = new ProcessBuilder("bash", LIFECYCLE.toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.environment().clear();
        builder.environment().putAll(environment);
        int exit = builder.start().waitFor();

        assertThat(exit).isEqualTo(2);
        assertThat(marker).doesNotExist();
    }
}
