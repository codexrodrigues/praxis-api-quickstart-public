package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    @Test
    void shouldUseTheCanonicalPublicOriginAcrossPublishedOperationalSmokes() throws Exception {
        List<String> operationalSurfaces = List.of(
                ".github/workflows/domain-catalog-runtime-smoke.yml",
                ".github/workflows/domain-rules-runtime-smoke.yml",
                ".github/workflows/sync-published-domain-catalog.yml",
                "scripts/upload-domain-catalog.sh",
                "scripts/ensure-domain-catalog-context.sh",
                "scripts/verify-cockpit-option-source-contracts.sh",
                "scripts/verify-domain-catalog-authoring-runtime.sh",
                "scripts/verify-domain-catalog-context.sh",
                "scripts/verify-domain-catalog-ingest-resilience.sh",
                "scripts/verify-domain-federation-runtime.sh",
                "scripts/verify-domain-knowledge-change-set-runtime.sh",
                "scripts/verify-domain-rules-runtime.sh");

        for (String surface : operationalSurfaces) {
            assertThat(normalizedContents(surface))
                    .as(surface)
                    .contains("https://praxisui.dev")
                    .doesNotContain("https://praxisui-dev.web.app");
        }
    }

    @Test
    void shouldAuthenticateTheHumanResourcesContextualActionProbe() throws Exception {
        String script = normalizedContents("scripts/verify-human-resources-runtime.sh");
        String workflow = normalizedContents(".github/workflows/domain-catalog-runtime-smoke.yml");

        assertThat(script)
                .contains("ADMIN_PASSWORD=\"${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-}}\"")
                .contains("${BACKEND_URL%/}/auth/login")
                .contains("-c \"$auth_cookie_jar\"")
                .contains("-b \"$auth_cookie_jar\"")
                .contains("authenticate_context_principal\nget_authenticated_json \"/api/human-resources/funcionarios/1/actions\"")
                .doesNotContain("get_json \"/api/human-resources/funcionarios/1/actions\"");
        assertThat(workflow)
                .contains("PRACTICE_TEMP_PASSWORD: ${{ secrets.PRACTICE_TEMP_PASSWORD }}")
                .contains("run: scripts/verify-human-resources-runtime.sh");
    }

    private static String normalizedContents(String path) throws Exception {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }
}
