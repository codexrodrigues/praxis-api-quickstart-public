package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationTestIsolationPolicyTest {

    private static final Path TEST_SOURCES = Path.of("src/test/java");

    @Test
    void springIntegrationTestsShouldNotUseDefaultRemoteDatasources() throws IOException {
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(TEST_SOURCES)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspectTestSource(path, violations));
        }

        assertThat(violations)
                .as("Spring integration tests must use local H2 datasources or be explicitly marked as external smoke tests")
                .isEmpty();
    }

    private static void inspectTestSource(Path path, List<String> violations) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException ex) {
            violations.add(path + " could not be read: " + ex.getMessage());
            return;
        }

        boolean springIntegrationTest = source.contains("@SpringBootTest")
                || source.contains("extends AbstractAuthenticatedHttpTest");
        if (!springIntegrationTest || path.endsWith("AbstractAuthenticatedHttpTest.java")) {
            return;
        }

        if (source.contains("@EnabledIfEnvironmentVariable(named = \"PRAXIS_EXTERNAL_SMOKE_TESTS\"")) {
            return;
        }

        boolean usesH2ApiDatasource = source.contains("spring.datasource.url=jdbc:h2:mem:");
        boolean usesH2ConfigDatasource = source.contains("config.datasource.url=jdbc:h2:mem:");
        boolean disablesFlyway = source.contains("spring.flyway.enabled=false");

        if (!usesH2ApiDatasource || !usesH2ConfigDatasource || !disablesFlyway) {
            violations.add(path + " must configure H2 api/config datasources and disable Flyway");
        }
    }
}
