package com.example.praxis.apiquickstart.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "PRAXIS_EXTERNAL_SMOKE_TESTS", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18088",
                "app.openapi.internal-base-url=http://localhost:18088",
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.csrf.disable=false",
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
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false"
        }
)
class StatsSchemaSmokeHttpTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldResolveRequestAndResponseSchemasForStatsResources() {
        ResponseEntity<Map> requestSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/human-resources/funcionarios/stats/group-by"
        );
        assertEquals(HttpStatus.OK, requestSchemaResponse.getStatusCode());
        assertHasProperty(requestSchemaResponse.getBody(), "filter");
        assertHasProperty(requestSchemaResponse.getBody(), "field");
        assertHasOperationExamples(requestSchemaResponse.getBody(), "request");

        ResponseEntity<Map> timeSeriesRequestSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, timeSeriesRequestSchemaResponse.getStatusCode());
        assertHasProperty(timeSeriesRequestSchemaResponse.getBody(), "field");
        assertHasProperty(timeSeriesRequestSchemaResponse.getBody(), "granularity");
        assertHasProperty(timeSeriesRequestSchemaResponse.getBody(), "metric");
        assertHasOperationExamples(timeSeriesRequestSchemaResponse.getBody(), "request");

        ResponseEntity<Map> distributionRequestSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/stats/distribution"
        );
        assertEquals(HttpStatus.OK, distributionRequestSchemaResponse.getStatusCode());
        assertHasProperty(distributionRequestSchemaResponse.getBody(), "field");
        assertHasProperty(distributionRequestSchemaResponse.getBody(), "mode");
        assertHasProperty(distributionRequestSchemaResponse.getBody(), "metric");
        assertHasOperationExamples(distributionRequestSchemaResponse.getBody(), "request");

        ResponseEntity<Map> payrollTimeSeriesRequestSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, payrollTimeSeriesRequestSchemaResponse.getStatusCode());
        assertHasProperty(payrollTimeSeriesRequestSchemaResponse.getBody(), "field");
        assertHasProperty(payrollTimeSeriesRequestSchemaResponse.getBody(), "granularity");
        assertHasProperty(payrollTimeSeriesRequestSchemaResponse.getBody(), "metric");
        assertHasProperty(payrollTimeSeriesRequestSchemaResponse.getBody(), "metrics");
        assertHasOperationExamples(payrollTimeSeriesRequestSchemaResponse.getBody(), "request");

        ResponseEntity<Map> groupByResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/stats/group-by"
        );
        assertEquals(HttpStatus.OK, groupByResponse.getStatusCode());
        assertHasProperty(groupByResponse.getBody(), "field");
        assertHasProperty(groupByResponse.getBody(), "buckets");
        assertHasOperationExamples(groupByResponse.getBody(), "response");

        ResponseEntity<Map> timeSeriesResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, timeSeriesResponse.getStatusCode());
        assertHasProperty(timeSeriesResponse.getBody(), "field");
        assertHasProperty(timeSeriesResponse.getBody(), "points");
        assertHasOperationExamples(timeSeriesResponse.getBody(), "response");

        ResponseEntity<Map> payrollTimeSeriesResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, payrollTimeSeriesResponse.getStatusCode());
        assertHasProperty(payrollTimeSeriesResponse.getBody(), "field");
        assertHasProperty(payrollTimeSeriesResponse.getBody(), "metrics");
        assertHasProperty(payrollTimeSeriesResponse.getBody(), "points");
        assertHasOperationExamples(payrollTimeSeriesResponse.getBody(), "response");

        ResponseEntity<Map> distributionResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/stats/distribution"
        );
        assertEquals(HttpStatus.OK, distributionResponse.getStatusCode());
        assertHasProperty(distributionResponse.getBody(), "field");
        assertHasProperty(distributionResponse.getBody(), "buckets");
        assertHasOperationExamples(distributionResponse.getBody(), "response");

        ResponseEntity<Map> payrollDistributionResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/distribution"
        );
        assertEquals(HttpStatus.OK, payrollDistributionResponse.getStatusCode());
        assertHasProperty(payrollDistributionResponse.getBody(), "field");
        assertHasProperty(payrollDistributionResponse.getBody(), "buckets");
        assertHasOperationExamples(payrollDistributionResponse.getBody(), "response");
    }

    @Test
    void shouldPublishResolvableSchemaLinksInCatalogForStatsOperations() {
        ResponseEntity<Map> perfilCatalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/stats/group-by"
        );
        assertEquals(HttpStatus.OK, perfilCatalogResponse.getStatusCode());
        Map<String, Object> perfilEndpoint = firstEndpoint(perfilCatalogResponse.getBody());
        assertEquals("/api/human-resources/vw-perfil-heroi/stats/group-by", perfilEndpoint.get("path"));
        assertEquals("api-human-resources-vw-perfil-heroi", perfilCatalogResponse.getBody().get("group"));
        assertSchemaName(perfilEndpoint, "GroupByStatsResponse");
        assertSchemaLink(perfilEndpoint, "response",
                "/schemas/filtered?path=%2Fapi%2Fhuman-resources%2Fvw-perfil-heroi%2Fstats%2Fgroup-by&operation=post&schemaType=response");

        ResponseEntity<Map> incidentesCatalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, incidentesCatalogResponse.getStatusCode());
        Map<String, Object> incidentesEndpoint = firstEndpoint(incidentesCatalogResponse.getBody());
        assertEquals("/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries", incidentesEndpoint.get("path"));
        assertEquals("api-risk-intelligence-vw-indicadores-incidentes", incidentesCatalogResponse.getBody().get("group"));
        assertSchemaName(incidentesEndpoint, "TimeSeriesStatsResponse");
        assertSchemaLink(incidentesEndpoint, "response",
                "/schemas/filtered?path=%2Fapi%2Frisk-intelligence%2Fvw-indicadores-incidentes%2Fstats%2Ftimeseries&operation=post&schemaType=response");

        ResponseEntity<Map> distributionCatalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/stats/distribution"
        );
        assertEquals(HttpStatus.OK, distributionCatalogResponse.getStatusCode());
        Map<String, Object> distributionEndpoint = firstEndpoint(distributionCatalogResponse.getBody());
        assertEquals("/api/human-resources/vw-perfil-heroi/stats/distribution", distributionEndpoint.get("path"));
        assertEquals("api-human-resources-vw-perfil-heroi", distributionCatalogResponse.getBody().get("group"));
        assertSchemaName(distributionEndpoint, "DistributionStatsResponse");
        assertSchemaLink(distributionEndpoint, "response",
                "/schemas/filtered?path=%2Fapi%2Fhuman-resources%2Fvw-perfil-heroi%2Fstats%2Fdistribution&operation=post&schemaType=response");

        ResponseEntity<Map> payrollCatalogResponse = restTemplate.getForEntity(
                "/schemas/catalog?path={path}&operation=post",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries"
        );
        assertEquals(HttpStatus.OK, payrollCatalogResponse.getStatusCode());
        Map<String, Object> payrollEndpoint = firstEndpoint(payrollCatalogResponse.getBody());
        assertEquals("/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries", payrollEndpoint.get("path"));
        assertEquals("api-human-resources-vw-analytics-folha-pagamento", payrollCatalogResponse.getBody().get("group"));
        assertSchemaName(payrollEndpoint, "TimeSeriesStatsResponse");
        assertSchemaLink(payrollEndpoint, "response",
                "/schemas/filtered?path=%2Fapi%2Fhuman-resources%2Fvw-analytics-folha-pagamento%2Fstats%2Ftimeseries&operation=post&schemaType=response");
    }

    @Test
    void shouldPublishOptionSourceMetadataForAnalyticalFilters() {
        ResponseEntity<Map> payrollFilterSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/human-resources/vw-analytics-folha-pagamento/filter"
        );
        assertEquals(HttpStatus.OK, payrollFilterSchemaResponse.getStatusCode());
        assertResourceCapability(payrollFilterSchemaResponse.getBody(), "optionSources", true);
        assertFieldOptionSource(payrollFilterSchemaResponse.getBody(), "universo", "universo", "DISTINCT_DIMENSION");
        assertFieldOptionSource(payrollFilterSchemaResponse.getBody(), "payrollProfile", "payrollProfile", "DISTINCT_DIMENSION");
        assertFieldOptionSourceDependencyFilterMap(
                payrollFilterSchemaResponse.getBody(),
                "payrollProfile",
                Map.of("universo", "universoContexto")
        );
        assertFieldOptionSource(payrollFilterSchemaResponse.getBody(), "faixaPctDesconto", "faixaPctDesconto", "CATEGORICAL_BUCKET");
        assertFieldEndpointContains(payrollFilterSchemaResponse.getBody(), "payrollProfile",
                "/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/filter");

        ResponseEntity<Map> heroiFilterSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/human-resources/vw-perfil-heroi/filter"
        );
        assertEquals(HttpStatus.OK, heroiFilterSchemaResponse.getStatusCode());
        assertFieldOptionSource(heroiFilterSchemaResponse.getBody(), "universo", "universo", "DISTINCT_DIMENSION");
        assertFieldOptionSource(heroiFilterSchemaResponse.getBody(), "basePrincipal", "basePrincipal", "DISTINCT_DIMENSION");

        ResponseEntity<Map> incidentesFilterSchemaResponse = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                Map.class,
                "/api/risk-intelligence/vw-indicadores-incidentes/filter"
        );
        assertEquals(HttpStatus.OK, incidentesFilterSchemaResponse.getStatusCode());
        assertFieldOptionSource(incidentesFilterSchemaResponse.getBody(), "severidade", "severidade", "DISTINCT_DIMENSION");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEndpoint(Map body) {
        assertNotNull(body);
        Object endpoints = body.get("endpoints");
        assertTrue(endpoints instanceof java.util.List<?>);
        java.util.List<?> endpointList = (java.util.List<?>) endpoints;
        assertTrue(!endpointList.isEmpty());
        return (Map<String, Object>) endpointList.get(0);
    }

    @SuppressWarnings("unchecked")
    private void assertHasProperty(Map body, String propertyName) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        assertTrue(((Map<String, Object>) properties).containsKey(propertyName));
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
    private void assertSchemaName(Map<String, Object> endpoint, String expectedName) {
        Object responseSchema = endpoint.get("responseSchema");
        assertTrue(responseSchema instanceof Map<?, ?>);
        assertEquals(expectedName, ((Map<String, Object>) responseSchema).get("name"));
        assertEquals("application/json", ((Map<String, Object>) responseSchema).get("mediaType"));
    }

    @SuppressWarnings("unchecked")
    private void assertSchemaLink(Map<String, Object> endpoint, String key, String expectedValue) {
        Object schemaLinks = endpoint.get("schemaLinks");
        assertTrue(schemaLinks instanceof Map<?, ?>);
        assertEquals(expectedValue, ((Map<String, Object>) schemaLinks).get(key));
    }

    @SuppressWarnings("unchecked")
    private void assertFieldOptionSource(Map body, String fieldName, String expectedKey, String expectedType) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        Object field = ((Map<String, Object>) properties).get(fieldName);
        assertTrue(field instanceof Map<?, ?>);
        Object xUiObj = ((Map<String, Object>) field).get("x-ui");
        assertTrue(xUiObj instanceof Map<?, ?>);
        Object optionSourceObj = ((Map<String, Object>) xUiObj).get("optionSource");
        assertTrue(optionSourceObj instanceof Map<?, ?>);
        assertEquals(expectedKey, ((Map<String, Object>) optionSourceObj).get("key"));
        assertEquals(expectedType, ((Map<String, Object>) optionSourceObj).get("type"));
    }

    @SuppressWarnings("unchecked")
    private void assertFieldOptionSourceDependencyFilterMap(
            Map body,
            String fieldName,
            Map<String, String> expectedMap
    ) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        Object field = ((Map<String, Object>) properties).get(fieldName);
        assertTrue(field instanceof Map<?, ?>);
        Object xUiObj = ((Map<String, Object>) field).get("x-ui");
        assertTrue(xUiObj instanceof Map<?, ?>);
        Object optionSourceObj = ((Map<String, Object>) xUiObj).get("optionSource");
        assertTrue(optionSourceObj instanceof Map<?, ?>);
        assertEquals(expectedMap, ((Map<String, Object>) optionSourceObj).get("dependencyFilterMap"));
    }

    @SuppressWarnings("unchecked")
    private void assertFieldEndpointContains(Map body, String fieldName, String expectedFragment) {
        assertNotNull(body);
        Object properties = body.get("properties");
        assertTrue(properties instanceof Map<?, ?>);
        Object field = ((Map<String, Object>) properties).get(fieldName);
        assertTrue(field instanceof Map<?, ?>);
        Object xUiObj = ((Map<String, Object>) field).get("x-ui");
        assertTrue(xUiObj instanceof Map<?, ?>);
        Object endpoint = ((Map<String, Object>) xUiObj).get("endpoint");
        assertTrue(endpoint instanceof String);
        assertTrue(((String) endpoint).contains(expectedFragment));
    }

    @SuppressWarnings("unchecked")
    private void assertResourceCapability(Map body, String capabilityKey, boolean expected) {
        assertNotNull(body);
        Object xUiObj = body.get("x-ui");
        assertTrue(xUiObj instanceof Map<?, ?>);
        Object resourceObj = ((Map<String, Object>) xUiObj).get("resource");
        assertTrue(resourceObj instanceof Map<?, ?>);
        Object capabilitiesObj = ((Map<String, Object>) resourceObj).get("capabilities");
        assertTrue(capabilitiesObj instanceof Map<?, ?>);
        assertEquals(expected, ((Map<String, Object>) capabilitiesObj).get(capabilityKey));
    }
}
