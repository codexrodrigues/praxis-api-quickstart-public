package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PublicSnapshotWorkflowContractTest {
    @Test
    void shouldVerifyThePublicTagWithoutPrivateWorkflowDependencies() throws Exception {
        Path template = Path.of(".github/public-workflows/ci-java.yml");
        Path workflow = Files.exists(template) ? template : Path.of(".github/workflows/ci-java.yml");
        String source = Files.readString(workflow).replace("\r\n", "\n");
        assertThat(source)
                .contains("tags:", "- v*", "git merge-base --is-ancestor HEAD origin/main")
                .contains("./mvnw -B -fae -Dstyle.color=always verify")
                .contains("bash scripts/verify-packaged-runtime.sh")
                .doesNotContain("prepare-public-release", "workflow_run:", "schedule:", "pull_request:", "branches:");
    }
}
