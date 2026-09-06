package com.example.praxis.apiquickstart.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperIdentityPreservationContractTest {

    private static final Path SOURCE_MAPPERS = Path.of("src", "main", "java");
    private static final Path GENERATED_MAPPERS = Path.of("target", "generated-sources", "annotations");
    private static final Pattern GENERATED_ID_REASSIGNMENT = Pattern.compile(
            "target\\.setId\\(\\s*source\\.getId\\(\\)\\s*\\)"
    );

    @Test
    void generatedUpdateMappersMustPreserveManagedEntityIdentity() throws IOException {
        List<Path> mapperImplementations;
        try (Stream<Path> paths = Files.walk(GENERATED_MAPPERS)) {
            mapperImplementations = paths
                    .filter(path -> path.getFileName().toString().endsWith("MapperImpl.java"))
                    .toList();
        }

        assertFalse(mapperImplementations.isEmpty(), "MapStruct generated no mapper implementations to audit");
        List<String> violations = mapperImplementations.stream()
                .filter(this::reassignsIdFromUpdateSource)
                .map(GENERATED_MAPPERS::relativize)
                .map(Path::toString)
                .sorted()
                .toList();

        assertTrue(violations.isEmpty(), () ->
                "Update mappers must keep route-owned entity identity; generated id assignments: " + violations);
    }

    @Test
    void sourceMappersMustNotCreateDetachedFuncionarioStubs() throws IOException {
        List<String> violations;
        try (Stream<Path> paths = Files.walk(SOURCE_MAPPERS)) {
            violations = paths
                    .filter(path -> path.getFileName().toString().endsWith("Mapper.java"))
                    .filter(this::createsDetachedFuncionarioStub)
                    .map(SOURCE_MAPPERS::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(violations.isEmpty(), () ->
                "Versioned Funcionario relationships must use ManagedEntityReferenceResolver: " + violations);
    }

    private boolean reassignsIdFromUpdateSource(Path mapperImplementation) {
        try {
            return GENERATED_ID_REASSIGNMENT.matcher(Files.readString(mapperImplementation)).find();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not audit generated mapper " + mapperImplementation, exception);
        }
    }

    private boolean createsDetachedFuncionarioStub(Path mapperSource) {
        try {
            return Files.readString(mapperSource).contains("new Funcionario()");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not audit source mapper " + mapperSource, exception);
        }
    }
}
