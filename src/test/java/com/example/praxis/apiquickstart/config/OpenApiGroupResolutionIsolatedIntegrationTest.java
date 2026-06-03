package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.praxisplatform.config.service.DomainCatalogSchemaValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_metadata_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_metadata_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class OpenApiGroupResolutionIsolatedIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DomainCatalogSchemaValidationService domainCatalogSchemaValidationService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldResolveDedicatedCrudGroupAndServeRequestSchema() {
        String path = "/api/human-resources/funcionarios";

        ResponseEntity<Map> catalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                path
        );
        assertEquals(HttpStatus.OK, catalogResponse.getStatusCode());
        assertNotNull(catalogResponse.getBody());
        assertEquals("api-human-resources-funcionarios", catalogResponse.getBody().get("group"));

        Map<String, Object> endpoint = firstEndpoint(catalogResponse.getBody());
        assertEquals(path, endpoint.get("path"));
        assertSchemaLink(endpoint, "request",
                "/schemas/filtered?path=%2Fapi%2Fhuman-resources%2Ffuncionarios&operation=post&schemaType=request");

        ResponseEntity<Map> groupDocResponse = restTemplate.getForEntity(
                "/v3/api-docs/{group}",
                Map.class,
                "api-human-resources-funcionarios"
        );
        assertEquals(HttpStatus.OK, groupDocResponse.getStatusCode());
        assertHasPath(groupDocResponse.getBody(), path);

        ResponseEntity<Map> filteredSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                path
        );
        assertEquals(HttpStatus.OK, filteredSchemaResponse.getStatusCode());
        assertHasProperty(filteredSchemaResponse.getBody(), "nomeCompleto");
        assertHasProperty(filteredSchemaResponse.getBody(), "cpf");
        assertHasProperty(filteredSchemaResponse.getBody(), "cargoId");
        assertHasProperty(filteredSchemaResponse.getBody(), "departamentoId");
        assertHasProperty(filteredSchemaResponse.getBody(), "dataNascimento");
        assertHasProperty(filteredSchemaResponse.getBody(), "dataAdmissao");
        assertHasNoProperty(filteredSchemaResponse.getBody(), "cargoIdsIn");
        assertHasNoProperty(filteredSchemaResponse.getBody(), "departamentoIdsIn");
        assertHasXUi(filteredSchemaResponse.getBody());

        ResponseEntity<Map> filterSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                path + "/filter"
        );
        assertEquals(HttpStatus.OK, filterSchemaResponse.getStatusCode());
        assertHasProperty(filterSchemaResponse.getBody(), "nomeCompleto");
        assertHasProperty(filterSchemaResponse.getBody(), "cpf");
        assertHasProperty(filterSchemaResponse.getBody(), "cargoIdsIn");
        assertHasProperty(filterSchemaResponse.getBody(), "departamentoIdsIn");
        assertHasNoProperty(filterSchemaResponse.getBody(), "cargoId");
        assertHasNoProperty(filterSchemaResponse.getBody(), "departamentoId");
        assertHasProperty(filterSchemaResponse.getBody(), "dataNascimentoRange");
        assertHasProperty(filterSchemaResponse.getBody(), "dataAdmissaoRange");
        assertHasProperty(filterSchemaResponse.getBody(), "dataAdmissaoLastDays");
        assertHasNoProperty(filterSchemaResponse.getBody(), "dataNascimentoOn");
        assertHasNoProperty(filterSchemaResponse.getBody(), "dataAdmissaoOn");
        assertHasXUi(filterSchemaResponse.getBody());
    }

    @Test
    void shouldExposeDomainGovernanceForSensitiveHrFields() {
        ResponseEntity<Map> domainResponse = restTemplate.getForEntity(
                "/schemas/domain?resourceKey={resourceKey}",
                Map.class,
                "human-resources.funcionarios"
        );
        assertEquals(HttpStatus.OK, domainResponse.getStatusCode());
        assertNotNull(domainResponse.getBody());
        assertEquals("praxis.domain-catalog/v0.2", domainResponse.getBody().get("schemaVersion"));

        Map<String, Object> cpfGovernance = findGovernanceItem(
                domainResponse.getBody(),
                "human-resources.funcionarios.field.cpf"
        );
        assertEquals("privacy", cpfGovernance.get("annotationType"));
        assertEquals("confidential", cpfGovernance.get("classification"));
        assertEquals("personal", cpfGovernance.get("dataCategory"));

        @SuppressWarnings("unchecked")
        List<String> complianceTags = (List<String>) cpfGovernance.get("complianceTags");
        assertTrue(complianceTags.contains("LGPD"));

        @SuppressWarnings("unchecked")
        Map<String, Object> aiUsage = (Map<String, Object>) cpfGovernance.get("aiUsage");
        assertNotNull(aiUsage);
        assertEquals("mask", aiUsage.get("visibility"));
        assertEquals("deny", aiUsage.get("trainingUse"));
        assertEquals("review_required", aiUsage.get("ruleAuthoring"));

        Map<String, Object> cpfNode = findDomainNode(
                domainResponse.getBody(),
                "human-resources.funcionarios.field.cpf"
        );
        assertEquals("active", cpfNode.get("lifecycle"));
        assertEquals("human-resources", cpfNode.get("semanticOwner"));

        @SuppressWarnings("unchecked")
        Map<String, Object> resolution = (Map<String, Object>) cpfNode.get("resolution");
        assertNotNull(resolution);
        assertEquals("human-resources.funcionarios.field.cpf", resolution.get("canonicalKey"));
        assertEquals("exact-key-or-alias", resolution.get("ambiguityPolicy"));

        @SuppressWarnings("unchecked")
        List<String> sourceEvidenceKeys = (List<String>) cpfNode.get("sourceEvidenceKeys");
        assertNotNull(sourceEvidenceKeys);
        assertFalse(sourceEvidenceKeys.isEmpty());
    }

    @Test
    void shouldEmitDomainCatalogPayloadsAcceptedByConfigContract() {
        List<String> resourceKeys = List.of(
                "human-resources.funcionarios",
                "human-resources.folhas-pagamento",
                "operations.missoes",
                "operations.acordos-regulatorios",
                "procurement.suppliers"
        );

        for (String resourceKey : resourceKeys) {
            ResponseEntity<Map> domainResponse = restTemplate.getForEntity(
                    "/schemas/domain?resourceKey={resourceKey}",
                    Map.class,
                    resourceKey
            );
            assertEquals(HttpStatus.OK, domainResponse.getStatusCode(), resourceKey);
            assertNotNull(domainResponse.getBody(), resourceKey);
            assertEquals("praxis.domain-catalog/v0.2", domainResponse.getBody().get("schemaVersion"), resourceKey);

            JsonNode payload = objectMapper.valueToTree(domainResponse.getBody());
            assertDoesNotThrow(
                    () -> domainCatalogSchemaValidationService.validate(payload),
                    () -> "Domain catalog contract mismatch for " + resourceKey
            );
            assertAiReadyDomainCatalog(resourceKey, payload);
        }
    }

    @Test
    void shouldExposeAiReadyRelationshipMapForMissionDomain() {
        ResponseEntity<Map> domainResponse = restTemplate.getForEntity(
                "/schemas/domain?resourceKey={resourceKey}",
                Map.class,
                "operations.missoes"
        );
        assertEquals(HttpStatus.OK, domainResponse.getStatusCode());
        assertNotNull(domainResponse.getBody());

        JsonNode payload = objectMapper.valueToTree(domainResponse.getBody());
        assertAiReadyDomainCatalog("operations.missoes", payload);

        JsonNode edges = payload.path("edges");
        assertFalse(edges.isEmpty(), "operations.missoes must publish semantic relationships");
        assertTrue(hasEdgeTouching("operations.missoes", edges), "mission resource must be present in relationship map");
        for (JsonNode edge : edges) {
            assertFalse(edge.path("edgeKey").asText().isBlank());
            assertFalse(edge.path("sourceNodeKey").asText().isBlank());
            assertFalse(edge.path("targetNodeKey").asText().isBlank());
            assertFalse(edge.path("edgeType").asText().isBlank());
            assertFalse(edge.path("evidenceKeys").isEmpty(), edge.path("edgeKey").asText());
        }
    }

    @Test
    void shouldResolveStatsGroupAndServeResponseSchemaFromCatalogLink() {
        String path = "/api/human-resources/vw-perfil-heroi/stats/group-by";

        ResponseEntity<Map> catalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                path
        );
        assertEquals(HttpStatus.OK, catalogResponse.getStatusCode());
        assertNotNull(catalogResponse.getBody());
        assertEquals("api-human-resources-vw-perfil-heroi", catalogResponse.getBody().get("group"));

        Map<String, Object> endpoint = firstEndpoint(catalogResponse.getBody());
        assertEquals(path, endpoint.get("path"));

        @SuppressWarnings("unchecked")
        Map<String, Object> schemaLinks = (Map<String, Object>) endpoint.get("schemaLinks");
        assertNotNull(schemaLinks);
        assertEquals(
                "/schemas/filtered?path=%2Fapi%2Fhuman-resources%2Fvw-perfil-heroi%2Fstats%2Fgroup-by&operation=post&schemaType=response",
                schemaLinks.get("response")
        );

        ResponseEntity<Map> groupDocResponse = restTemplate.getForEntity(
                "/v3/api-docs/{group}",
                Map.class,
                "api-human-resources-vw-perfil-heroi"
        );
        assertEquals(HttpStatus.OK, groupDocResponse.getStatusCode());
        assertHasPath(groupDocResponse.getBody(), path);

        ResponseEntity<Map> filteredSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                path
        );
        assertEquals(HttpStatus.OK, filteredSchemaResponse.getStatusCode());
        assertHasProperty(filteredSchemaResponse.getBody(), "field");
        assertHasProperty(filteredSchemaResponse.getBody(), "buckets");
        assertHasOperationExamples(filteredSchemaResponse.getBody(), "response");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeAnalyticsProjectionsForPilotStatsOperations() {
        ResponseEntity<Map> payrollSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/group-by"
        );
        assertEquals(HttpStatus.OK, payrollSchemaResponse.getStatusCode());
        Map<String, Object> payrollXUi = (Map<String, Object>) payrollSchemaResponse.getBody().get("x-ui");
        assertNotNull(payrollXUi);
        Map<String, Object> payrollAnalytics = (Map<String, Object>) payrollXUi.get("analytics");
        assertNotNull(payrollAnalytics);
        List<Map<String, Object>> payrollProjections = (List<Map<String, Object>>) payrollAnalytics.get("projections");
        assertEquals(3, payrollProjections.size());
        assertEquals("payroll-ranking-table", payrollProjections.get(0).get("id"));
        assertEquals("payroll-ranking-chart", payrollProjections.get(1).get("id"));
        assertEquals("payroll-profile-chart", payrollProjections.get(2).get("id"));
        Map<String, Object> payrollRankingBindings = (Map<String, Object>) payrollProjections.get(0).get("bindings");
        List<Map<String, Object>> payrollRankingMetrics = (List<Map<String, Object>>) payrollRankingBindings.get("primaryMetrics");
        assertEquals("salarioLiquido", payrollRankingMetrics.get(0).get("field"));
        Map<String, Object> payrollRankingDefaults = (Map<String, Object>) payrollProjections.get(0).get("defaults");
        List<Map<String, Object>> payrollRankingSort = (List<Map<String, Object>>) payrollRankingDefaults.get("sort");
        assertEquals("salarioLiquido", payrollRankingSort.get(0).get("field"));

        ResponseEntity<Map> payrollTrendSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, payrollTrendSchemaResponse.getStatusCode());
        Map<String, Object> payrollTrendXUi = (Map<String, Object>) payrollTrendSchemaResponse.getBody().get("x-ui");
        assertNotNull(payrollTrendXUi);
        Map<String, Object> payrollTrendAnalytics = (Map<String, Object>) payrollTrendXUi.get("analytics");
        assertNotNull(payrollTrendAnalytics);
        List<Map<String, Object>> payrollTrendProjections = (List<Map<String, Object>>) payrollTrendAnalytics.get("projections");
        assertEquals(1, payrollTrendProjections.size());
        assertEquals("payroll-trend-chart", payrollTrendProjections.get(0).get("id"));
        Map<String, Object> payrollTrendDefaults = (Map<String, Object>) payrollTrendProjections.get(0).get("defaults");
        assertEquals("month", payrollTrendDefaults.get("granularity"));

        ResponseEntity<Map> incidentSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, incidentSchemaResponse.getStatusCode());
        Map<String, Object> incidentXUi = (Map<String, Object>) incidentSchemaResponse.getBody().get("x-ui");
        assertNotNull(incidentXUi);
        Map<String, Object> incidentAnalytics = (Map<String, Object>) incidentXUi.get("analytics");
        assertNotNull(incidentAnalytics);
        List<Map<String, Object>> incidentProjections = (List<Map<String, Object>>) incidentAnalytics.get("projections");
        assertEquals(1, incidentProjections.size());
        assertEquals("incident-trend", incidentProjections.get(0).get("id"));
        Map<String, Object> incidentDefaults = (Map<String, Object>) incidentProjections.get(0).get("defaults");
        assertEquals("month", incidentDefaults.get("granularity"));
    }

    @Test
    void shouldResolveCanonicalIdFieldForRequestSchemaWithRelationIds() {
        String path = "/api/human-resources/funcionario-habilidades/filter";

        ResponseEntity<Map> filteredSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                path
        );
        assertEquals(HttpStatus.OK, filteredSchemaResponse.getStatusCode());
        assertHasProperty(filteredSchemaResponse.getBody(), "funcionarioId");
        assertHasProperty(filteredSchemaResponse.getBody(), "habilidadeId");

        @SuppressWarnings("unchecked")
        Map<String, Object> xUi = (Map<String, Object>) filteredSchemaResponse.getBody().get("x-ui");
        assertNotNull(xUi);

        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) xUi.get("resource");
        assertNotNull(resource);
        assertEquals("id", resource.get("idField"));
        assertEquals(Boolean.FALSE, resource.get("idFieldValid"));
    }

    @Test
    void shouldExposeFlattenedRestApiResourceShapeInGroupedOpenApiCollectionResponses() throws Exception {
        ResponseEntity<String> groupDocResponse = restTemplate.getForEntity(
                "/v3/api-docs/{group}",
                String.class,
                "api-human-resources-funcionarios"
        );
        assertEquals(HttpStatus.OK, groupDocResponse.getStatusCode());

        JsonNode root = objectMapper.readTree(groupDocResponse.getBody());

        JsonNode allRowSchema = resolveCollectionItemSchema(root, "/api/human-resources/funcionarios/all", "get");
        assertTrue(allRowSchema.path("properties").has("nomeCompleto"));
        assertTrue(allRowSchema.path("properties").has("cpf"));
        assertTrue(allRowSchema.path("properties").has("_links"));
        assertFalse(allRowSchema.path("properties").has("content"));
        assertFalse(allRowSchema.path("properties").has("links"));
        assertCanonicalLinksSchema(root, allRowSchema);

        JsonNode filterRowSchema = resolveCollectionItemSchema(root, "/api/human-resources/funcionarios/filter", "post");
        assertTrue(filterRowSchema.path("properties").has("nomeCompleto"));
        assertTrue(filterRowSchema.path("properties").has("_links"));
        assertFalse(filterRowSchema.path("properties").has("content"));
        assertCanonicalLinksSchema(root, filterRowSchema);

        JsonNode cursorRowSchema = resolveCollectionItemSchema(root, "/api/human-resources/funcionarios/filter/cursor", "post");
        assertTrue(cursorRowSchema.path("properties").has("nomeCompleto"));
        assertTrue(cursorRowSchema.path("properties").has("_links"));
        assertFalse(cursorRowSchema.path("properties").has("content"));
        assertCanonicalLinksSchema(root, cursorRowSchema);
    }

    @Test
    void shouldExposeCanonicalLinksShapeInGroupedOpenApiItemResponses() throws Exception {
        ResponseEntity<String> groupDocResponse = restTemplate.getForEntity(
                "/v3/api-docs/{group}",
                String.class,
                "api-human-resources-funcionarios"
        );
        assertEquals(HttpStatus.OK, groupDocResponse.getStatusCode());

        JsonNode root = objectMapper.readTree(groupDocResponse.getBody());
        JsonNode itemResponseSchema = resolveSchemaNode(
                root,
                root.path("paths")
                        .path("/api/human-resources/funcionarios/{id}")
                        .path("get")
                        .path("responses")
                        .path("200")
                        .path("content")
                        .path("application/json")
                        .path("schema")
        );
        JsonNode itemDataSchema = resolveSchemaNode(root, itemResponseSchema.path("properties").path("data"));

        assertTrue(itemResponseSchema.path("properties").has("_links"));
        assertTrue(itemDataSchema.path("properties").has("nomeCompleto"));
        assertTrue(itemDataSchema.path("properties").has("cpf"));
        assertFalse(itemDataSchema.path("properties").has("content"));
        assertCanonicalLinksSchema(root, itemResponseSchema);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEndpoint(Map body) {
        assertNotNull(body);
        Object endpoints = body.get("endpoints");
        assertTrue(endpoints instanceof List<?>);
        List<?> endpointList = (List<?>) endpoints;
        assertTrue(!endpointList.isEmpty());
        return (Map<String, Object>) endpointList.get(0);
    }

    @SuppressWarnings("unchecked")
    private void assertHasPath(Map body, String path) {
        assertNotNull(body);
        Object paths = body.get("paths");
        assertTrue(paths instanceof Map<?, ?>);
        assertTrue(((Map<String, Object>) paths).containsKey(path));
    }

    @SuppressWarnings("unchecked")
    private void assertHasProperty(Map body, String propertyName) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        assertTrue(((Map<String, Object>) properties).containsKey(propertyName));
    }

    @SuppressWarnings("unchecked")
    private void assertHasNoProperty(Map body, String propertyName) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        assertFalse(((Map<String, Object>) properties).containsKey(propertyName));
    }

    @SuppressWarnings("unchecked")
    private void assertHasXUi(Map body) {
        assertNotNull(body);
        Object xUi = body.get("x-ui");
        assertTrue(xUi instanceof Map<?, ?>);
        assertTrue(((Map<String, Object>) xUi).containsKey("resource"));
    }

    @SuppressWarnings("unchecked")
    private void assertHasOperationExamples(Map body, String exampleType) {
        assertNotNull(body);
        Object xUiObj = body.get("x-ui");
        assertTrue(xUiObj instanceof Map<?, ?>);
        Object operationExamples = ((Map<String, Object>) xUiObj).get("operationExamples");
        assertTrue(operationExamples instanceof Map<?, ?>);
        assertTrue(((Map<String, Object>) operationExamples).containsKey(exampleType));
    }

    @SuppressWarnings("unchecked")
    private void assertSchemaLink(Map<String, Object> endpoint, String key, String expectedValue) {
        Object schemaLinks = endpoint.get("schemaLinks");
        assertTrue(schemaLinks instanceof Map<?, ?>);
        assertEquals(expectedValue, ((Map<String, Object>) schemaLinks).get(key));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findGovernanceItem(Map body, String nodeKey) {
        assertNotNull(body);
        Object governance = body.get("governance");
        assertTrue(governance instanceof List<?>);
        for (Object item : (List<?>) governance) {
            assertTrue(item instanceof Map<?, ?>);
            Map<String, Object> map = (Map<String, Object>) item;
            if (nodeKey.equals(map.get("nodeKey"))) {
                return map;
            }
        }
        throw new AssertionError("Missing governance item for nodeKey " + nodeKey);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findDomainNode(Map body, String nodeKey) {
        assertNotNull(body);
        Object nodes = body.get("nodes");
        assertTrue(nodes instanceof List<?>);
        for (Object item : (List<?>) nodes) {
            assertTrue(item instanceof Map<?, ?>);
            Map<String, Object> map = (Map<String, Object>) item;
            if (nodeKey.equals(map.get("nodeKey"))) {
                return map;
            }
        }
        throw new AssertionError("Missing domain node for nodeKey " + nodeKey);
    }

    private void assertAiReadyDomainCatalog(String resourceKey, JsonNode payload) {
        assertEquals("praxis.domain-catalog/v0.2", payload.path("schemaVersion").asText(), resourceKey);
        assertFalse(payload.path("service").path("serviceKey").asText().isBlank(), resourceKey);
        assertTrue(payload.path("release").path("releaseKey").asText().contains(":" + resourceKey + ":"), resourceKey);
        assertFalse(payload.path("contexts").isEmpty(), resourceKey);
        assertFalse(payload.path("nodes").isEmpty(), resourceKey);
        assertFalse(payload.path("bindings").isEmpty(), resourceKey);
        assertFalse(payload.path("evidence").isEmpty(), resourceKey);
        assertFalse(payload.path("governance").isEmpty(), resourceKey);

        for (JsonNode governance : payload.path("governance")) {
            String nodeKey = governance.path("nodeKey").asText();
            assertFalse(nodeKey.isBlank(), resourceKey);
            assertFalse(governance.path("classification").asText().isBlank(), nodeKey);
            assertFalse(governance.path("dataCategory").asText().isBlank(), nodeKey);
            assertFalse(governance.path("complianceTags").isEmpty(), nodeKey);
            assertFalse(governance.path("aiUsage").path("visibility").asText().isBlank(), nodeKey);
            assertFalse(governance.path("aiUsage").path("trainingUse").asText().isBlank(), nodeKey);
            assertFalse(governance.path("aiUsage").path("ruleAuthoring").asText().isBlank(), nodeKey);
        }

        for (JsonNode evidence : payload.path("evidence")) {
            assertFalse(evidence.path("evidenceKey").asText().isBlank(), resourceKey);
            assertFalse(evidence.path("evidenceType").asText().isBlank(), resourceKey);
            assertFalse(evidence.path("summary").asText().isBlank(), evidence.path("evidenceKey").asText());
        }
    }

    private boolean hasEdgeTouching(String resourceKey, JsonNode edges) {
        for (JsonNode edge : edges) {
            if (edge.path("sourceNodeKey").asText().contains(resourceKey)
                    || edge.path("targetNodeKey").asText().contains(resourceKey)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode resolveCollectionItemSchema(JsonNode root, String path, String operation) {
        JsonNode responseSchema = resolveSchemaNode(
                root,
                root.path("paths")
                        .path(path)
                        .path(operation)
                        .path("responses")
                        .path("200")
                        .path("content")
                        .path("application/json")
                        .path("schema")
        );
        JsonNode dataSchema = resolveSchemaNode(root, responseSchema.path("properties").path("data"));
        JsonNode contentSchema = "array".equals(dataSchema.path("type").asText())
                ? dataSchema
                : resolveSchemaNode(root, dataSchema.path("properties").path("content"));
        assertEquals("array", contentSchema.path("type").asText());
        return resolveSchemaNode(root, contentSchema.path("items"));
    }

    private void assertCanonicalLinksSchema(JsonNode root, JsonNode rowSchema) {
        JsonNode linksSchema = resolveSchemaNode(root, rowSchema.path("properties").path("_links"));
        assertEquals("object", linksSchema.path("type").asText());
        assertFalse(linksSchema.path("properties").has("empty"));
        JsonNode additionalProperties = linksSchema.path("additionalProperties");
        assertTrue(additionalProperties.has("oneOf"));
        assertEquals(2, additionalProperties.path("oneOf").size());

        JsonNode singleLink = resolveSchemaNode(root, additionalProperties.path("oneOf").get(0));
        assertEquals("object", singleLink.path("type").asText());
        assertTrue(singleLink.path("properties").has("href"));

        JsonNode repeatedLinks = additionalProperties.path("oneOf").get(1);
        assertEquals("array", repeatedLinks.path("type").asText());
        JsonNode repeatedLinkItem = resolveSchemaNode(root, repeatedLinks.path("items"));
        assertTrue(repeatedLinkItem.path("properties").has("href"));
    }

    private JsonNode resolveSchemaNode(JsonNode root, JsonNode schemaNode) {
        if (schemaNode == null || schemaNode.isMissingNode() || schemaNode.isNull()) {
            return schemaNode;
        }
        if (schemaNode.has("$ref")) {
            return resolveSchemaNode(root, resolveRef(root, schemaNode.path("$ref").asText()));
        }
        if (schemaNode.has("allOf") && schemaNode.path("allOf").isArray() && !schemaNode.path("allOf").isEmpty()) {
            com.fasterxml.jackson.databind.node.ObjectNode merged = objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode mergedProperties = objectMapper.createObjectNode();
            for (JsonNode candidate : schemaNode.path("allOf")) {
                JsonNode resolved = resolveSchemaNode(root, candidate);
                if (resolved != null && resolved.has("properties")) {
                    resolved.path("properties").fields()
                            .forEachRemaining(entry -> mergedProperties.set(entry.getKey(), entry.getValue()));
                }
            }
            if (!mergedProperties.isEmpty()) {
                merged.set("properties", mergedProperties);
                return merged;
            }
        }
        return schemaNode;
    }

    private JsonNode resolveRef(JsonNode root, String ref) {
        assertTrue(ref.startsWith("#/"));
        JsonNode current = root;
        for (String token : ref.substring(2).split("/")) {
            current = current.path(token);
        }
        assertFalse(current.isMissingNode(), "Missing component for ref " + ref);
        return current;
    }
}
