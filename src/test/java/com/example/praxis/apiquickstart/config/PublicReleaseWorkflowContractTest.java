package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PublicReleaseWorkflowContractTest {

    @Test
    void shouldPublishOnlyFromAReproducibleSourceTag() throws Exception {
        String workflow = normalizedContents(".github/workflows/publish-public-release.yml");

        assertThat(workflow)
                .contains("tags:\n      - 'v*'")
                .contains("contents: read")
                .contains("ssh-key: ${{ secrets.SOURCE_RELEASE_SSH_KEY }}")
                .contains("ssh-strict: true")
                .contains("run: mvn -B verify")
                .contains("mvn -q versions:set -DnewVersion=\"$VERSION\"")
                .contains("git push --atomic origin \"HEAD:${GITHUB_REF_NAME}\" \"$TAG\"")
                .contains("if: github.event_name == 'push' && startsWith(github.ref, 'refs/tags/v')")
                .contains("Tagged pom.xml version '$POM_VERSION' does not match '$TAG_VERSION'.")
                .contains("git push --atomic origin \"HEAD:${PUBLIC_BRANCH}\" \"$RELEASE_NAME\"");
        assertThat(workflow)
                .doesNotContain("branches:\n      - main")
                .doesNotContain("github.event.release")
                .doesNotContain("release_ref")
                .doesNotContain("RELEASE_PAT")
                .doesNotContain("contents: write");
    }

    @Test
    void shouldTriggerPostRolloutSmokeFromTheTagPublicationRun() throws Exception {
        String smokeWorkflow = normalizedContents(".github/workflows/domain-catalog-runtime-smoke.yml");

        assertThat(smokeWorkflow)
                .contains("workflows: [Publish Public Release]")
                .contains("startsWith(github.event.workflow_run.head_branch, 'v')")
                .doesNotContain("branches: [main]");
    }

    private static String normalizedContents(String path) throws Exception {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }
}
