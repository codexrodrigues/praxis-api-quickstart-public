package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

class FilterDtoRelationContractTest {

    @Test
    void listMembershipAliasesPointToRealEntityAttributes() throws Exception {
        List<String> violations = new ArrayList<>();

        for (Class<?> filterDto : loadFilterDtoClasses()) {
            for (Field field : filterDto.getDeclaredFields()) {
                Filterable filterable = field.getAnnotation(Filterable.class);
                if (filterable == null || !isListMembershipOperation(filterable.operation())) {
                    continue;
                }
                if (filterable.relation().isBlank()) {
                    violations.add(filterDto.getSimpleName() + "." + field.getName());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                () -> "IN/NOT_IN filter aliases must declare @Filterable(relation=...) so the JPA "
                        + "builder targets the real entity attribute instead of the DTO field: " + violations);
    }

    private static boolean isListMembershipOperation(Filterable.FilterOperation operation) {
        return operation == Filterable.FilterOperation.IN
                || operation == Filterable.FilterOperation.NOT_IN;
    }

    private static List<Class<?>> loadFilterDtoClasses() throws IOException, URISyntaxException, ClassNotFoundException {
        Path root = Path.of(Objects.requireNonNull(
                        FilterDtoRelationContractTest.class
                                .getClassLoader()
                                .getResource("com/example/praxis/apiquickstart"),
                        "Compiled quickstart classes must be available for FilterDTO contract scanning")
                .toURI());

        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith("FilterDTO.class") || fileName.contains("$")) {
                    continue;
                }

                String className = "com.example.praxis.apiquickstart."
                        + root.relativize(path)
                                .toString()
                                .replace('\\', '.')
                                .replace('/', '.')
                                .replaceAll("\\.class$", "");
                Class<?> candidate = Class.forName(className);
                if (GenericFilterDTO.class.isAssignableFrom(candidate)) {
                    classes.add(candidate);
                }
            }
        }
        return classes;
    }
}
