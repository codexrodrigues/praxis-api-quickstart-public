package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PublicDemoSeedOperationalSchemaContractTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path MAIN_SOURCES = PROJECT_ROOT.resolve("src/main/java/com/example/praxis/apiquickstart");
    private static final Path PUBLIC_DEMO_SEED = PROJECT_ROOT.resolve("db/dump/public-demo-seed.sql");
    private static final Pattern ENTITY_ANNOTATION = Pattern.compile("^\\s*@Entity\\b", Pattern.MULTILINE);
    private static final Pattern TABLE_ANNOTATION = Pattern.compile("@Table\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern TABLE_NAME = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");

    @Test
    void publicDemoSeedMaterializesAllQuickstartEntities() throws IOException {
        String dump = Files.readString(PUBLIC_DEMO_SEED, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        List<String> missingObjects = new ArrayList<>();

        for (Path source : quickstartJavaSources()) {
            String code = Files.readString(source, StandardCharsets.UTF_8);
            if (!ENTITY_ANNOTATION.matcher(code).find()) {
                continue;
            }

            String tableName = tableName(code);
            if (tableName == null || !dumpMaterializes(dump, tableName)) {
                missingObjects.add(MAIN_SOURCES.relativize(source) + " -> " + tableName);
            }
        }

        assertTrue(
                missingObjects.isEmpty(),
                "Every Quickstart @Entity must be materialized by db/dump/public-demo-seed.sql. Missing: "
                        + missingObjects);
    }

    private static List<Path> quickstartJavaSources() throws IOException {
        try (var stream = Files.walk(MAIN_SOURCES)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static String tableName(String code) {
        Matcher tableMatcher = TABLE_ANNOTATION.matcher(code);
        if (!tableMatcher.find()) {
            return null;
        }

        Matcher nameMatcher = TABLE_NAME.matcher(tableMatcher.group(1));
        if (!nameMatcher.find()) {
            return null;
        }
        return nameMatcher.group(1);
    }

    private static boolean dumpMaterializes(String dump, String tableName) {
        String normalized = tableName.toLowerCase(Locale.ROOT);
        return dump.contains("create table public." + normalized)
                || dump.contains("create view public." + normalized);
    }
}
