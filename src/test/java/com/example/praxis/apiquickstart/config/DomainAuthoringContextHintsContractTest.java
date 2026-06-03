package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DomainAuthoringContextHintsContractTest {

    private static final String DOMAIN_CATALOG_CONTEXT_HINT_SCHEMA_VERSION =
            "praxis.ai.context-hints.domain-catalog/v0.2";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path SCHEMA_PATH = Path.of(
            "docs/contracts/domain-authoring-context-hints.schema.json");
    private static final Path EXAMPLE_PATH = Path.of(
            "payloads/domain_authoring_context_hints.example.json");

    @Test
    void shouldKeepRequiredAuthoringContextHintsInSchemaAndExample() throws Exception {
        JsonNode schema = OBJECT_MAPPER.readTree(SCHEMA_PATH.toFile());
        JsonNode example = OBJECT_MAPPER.readTree(EXAMPLE_PATH.toFile());
        JsonNode domainCatalogSchema = schema.path("properties").path("domainCatalog");
        JsonNode domainCatalogExample = example.path("domainCatalog");

        assertEquals("object", schema.path("type").asText());
        assertEquals("object", domainCatalogSchema.path("type").asText());
        assertFalse(domainCatalogExample.isMissingNode());

        for (String requiredField : Set.of(
                "serviceKey",
                "resourceKey",
                "type",
                "query",
                "limit")) {
            assertContainsRequiredField(domainCatalogSchema, requiredField);
            assertFalse(domainCatalogExample.path(requiredField).isMissingNode(), requiredField);
            assertFalse(domainCatalogExample.path(requiredField).asText().isBlank(), requiredField);
        }

        JsonNode domainCatalogProperties = domainCatalogSchema.path("properties");
        assertEquals(
                DOMAIN_CATALOG_CONTEXT_HINT_SCHEMA_VERSION,
                domainCatalogProperties.path("schemaVersion").path("const").asText());
        assertEquals(
                DOMAIN_CATALOG_CONTEXT_HINT_SCHEMA_VERSION,
                domainCatalogExample.path("schemaVersion").asText());

        assertEnumContains(domainCatalogProperties.path("type").path("enum"), Set.of(
                "context",
                "node",
                "edge",
                "binding",
                "evidence",
                "governance",
                "vocabulary",
                "relationship"));
        assertEnumDoesNotContain(domainCatalogProperties.path("type").path("enum"), Set.of(
                "all",
                "concept"));
        assertEnumContains(
                domainCatalogProperties.path("intent").path("enum"),
                Set.of("authoring", "explain", "validate", "ai-access-control"));
        assertEnumContains(
                domainCatalogProperties.path("recommendedAuthoringFlow").path("enum"),
                Set.of("shared_rule_authoring", "component_authoring", "ui_composition_authoring"));
        assertEnumContains(
                domainCatalogProperties.path("recommendedRuleType").path("enum"),
                Set.of("privacy", "compliance", "validation", "selection_eligibility", "workflow_action_policy", "approval_policy"));

        assertEquals("authoring", domainCatalogExample.path("intent").asText());
        assertEquals("binding", domainCatalogExample.path("itemTypes").path(0).asText());
        assertFalse(domainCatalogExample.path("artifactKind").asText().isBlank());
        assertFalse(domainCatalogExample.path("targetLayer").asText().isBlank());
        assertEquals("shared_rule_authoring", domainCatalogExample.path("recommendedAuthoringFlow").asText());
        assertEquals("workflow_action_policy", domainCatalogExample.path("recommendedRuleType").asText());
        assertTrue(domainCatalogExample.path("relationships").path("enabled").asBoolean());
        assertTrue(domainCatalogExample.path("relationships").path("federated").asBoolean());
        assertFalse(domainCatalogExample.path("relationships").path("query").asText().isBlank());
        assertEquals(8, domainCatalogExample.path("relationships").path("limit").asInt());
        assertTrue(domainCatalogExample.path("governance").path("requireEvidence").asBoolean());
        assertTrue(domainCatalogExample.path("governance").path("requireHumanReview").asBoolean());
        assertTrue(domainCatalogExample.path("governance").path("respectAiUsage").asBoolean());
    }

    private static void assertContainsRequiredField(JsonNode schema, String fieldName) {
        for (JsonNode requiredField : schema.path("required")) {
            if (fieldName.equals(requiredField.asText())) {
                return;
            }
        }
        throw new AssertionError("Missing required field in schema: " + fieldName);
    }

    private static void assertEnumContains(JsonNode enumValues, Set<String> expectedValues) {
        for (String expectedValue : expectedValues) {
            boolean found = false;
            for (JsonNode enumValue : enumValues) {
                if (expectedValue.equals(enumValue.asText())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Missing enum value: " + expectedValue);
        }
    }

    private static void assertEnumDoesNotContain(JsonNode enumValues, Set<String> rejectedValues) {
        for (JsonNode enumValue : enumValues) {
            assertFalse(rejectedValues.contains(enumValue.asText()), "Unexpected enum value: " + enumValue.asText());
        }
    }
}
