package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.hr.analytics.demo-department-scopes=global=*,aggregate-only=*,demo-manager=1,blocked=",
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_absence_analytics_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_absence_analytics_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class VwAnalyticsAfastamentoComparisonHttpTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @BeforeEach
    void seedAnalyticsView() {
        jdbcTemplate.execute("drop table if exists public.vw_analytics_afastamentos");
        jdbcTemplate.execute("""
                create table public.vw_analytics_afastamentos (
                    analytics_id varchar(120) primary key,
                    funcionario_id integer not null,
                    departamento_id integer not null,
                    departamento_codigo varchar(40) not null,
                    departamento varchar(120) not null,
                    competencia date not null,
                    ano integer not null,
                    mes integer not null,
                    periodo_inicio date not null,
                    periodo_fim date not null,
                    dias_afastado bigint not null,
                    criticality_level varchar(40) not null,
                    criticality_policy_id varchar(80) not null,
                    criticality_policy_version varchar(40) not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.vw_analytics_afastamentos
                (analytics_id, funcionario_id, departamento_id, departamento_codigo, departamento, competencia, ano, mes, periodo_inicio, periodo_fim, dias_afastado, criticality_level, criticality_policy_id, criticality_policy_version)
                values
                ('cur-ops-1', 101, 1, 'OPS', 'Operacoes', DATE '2026-07-01', 2026, 7, DATE '2026-07-01', DATE '2026-07-10', 10, 'ATTENTION', 'hr-absence-criticality-v1', '2026-07-15'),
                ('cur-ops-2', 102, 1, 'OPS', 'Operacoes', DATE '2026-07-01', 2026, 7, DATE '2026-07-05', DATE '2026-07-20', 16, 'CRITICAL', 'hr-absence-criticality-v1', '2026-07-15'),
                ('prev-ops-1', 101, 1, 'OPS', 'Operacoes', DATE '2026-06-01', 2026, 6, DATE '2026-06-01', DATE '2026-06-05', 5, 'STANDARD', 'hr-absence-criticality-v1', '2026-07-15'),
                ('cur-hr-1', 201, 2, 'HR', 'Recursos Humanos', DATE '2026-07-01', 2026, 7, DATE '2026-07-01', DATE '2026-07-07', 7, 'ATTENTION', 'hr-absence-criticality-v1', '2026-07-15'),
                ('cur-hr-outside', 202, 2, 'HR', 'Recursos Humanos', DATE '2026-07-01', 2026, 7, DATE '2026-07-01', DATE '2026-07-08', 8, 'OUTSIDE_SCOPE_ONLY', 'hr-absence-criticality-v1', '2026-07-15')
                """);
    }

    @Test
    void shouldCompareAbsenceAnalyticsByEffectiveDepartment() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                "aggregate-only", "USER", java.util.List.of(HrAnalyticsAuthorities.AGGREGATE_READ)
        ));
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "filter": {},
                  "field": "departamento",
                  "periodField": "competencia",
                  "period": {
                    "from": "2026-07-01",
                    "to": "2026-07-31",
                    "timezone": "America/Sao_Paulo",
                    "mode": "PREVIOUS_ALIGNED"
                  },
                  "metrics": [
                    { "operation": "DISTINCT_COUNT", "field": "funcionarioId", "alias": "colaboradores" },
                    { "operation": "SUM", "field": "diasAfastado", "alias": "dias" }
                  ],
                  "orderBy": "VALUE_DESC",
                  "limit": 10
                }
                """, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/vw-analytics-afastamentos/stats/comparison",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("success", body.path("status").asText());
        JsonNode buckets = body.path("data").path("buckets");
        assertTrue(buckets.isArray());
        JsonNode operacoes = findBucket(buckets, 1);
        assertNotNull(operacoes);
        assertEquals("Operacoes", operacoes.path("label").asText());
        assertEquals(2, operacoes.path("values").path("colaboradores").path("current").asInt());
        assertEquals(1, operacoes.path("values").path("colaboradores").path("previous").asInt());
        assertEquals(26, operacoes.path("values").path("dias").path("current").asInt());
        assertEquals(5, operacoes.path("values").path("dias").path("previous").asInt());

        assertTrue(operacoes.path("key").isIntegralNumber());
        assertTrue(operacoes.path("funcionarioId").isMissingNode());

        var filterBody = objectMapper.createObjectNode();
        filterBody.putArray("departamentoIdsIn").add(operacoes.path("key").asInt());
        ResponseEntity<String> filterResponse = restTemplate.postForEntity(
                "/api/human-resources/vw-analytics-afastamentos/filter?page=0&size=20",
                new HttpEntity<>(filterBody.toString(), headersFor(
                        "aggregate-only",
                        HrAnalyticsAuthorities.AGGREGATE_READ,
                        HrAnalyticsAuthorities.NOMINAL_READ,
                        HrAnalyticsAuthorities.EMPLOYEE_360_READ
                )),
                String.class
        );
        assertEquals(HttpStatus.OK, filterResponse.getStatusCode(), filterResponse.getBody());
        JsonNode filteredRows = objectMapper.readTree(filterResponse.getBody()).path("data").path("content");
        assertEquals(3, filteredRows.size());
        filteredRows.forEach(row -> {
            assertEquals(operacoes.path("key").asInt(), row.path("departamentoId").asInt());
            assertEquals("Operacoes", row.path("departamento").asText());
            assertTrue(row.path("funcionarioId").isIntegralNumber());
        });
    }

    @Test
    void shouldKeepIncludedAbsenceRowsInsideTheServerResolvedResourceScope() throws Exception {
        String filterPath = "/api/human-resources/vw-analytics-afastamentos/filter?page=0&size=20";
        HttpHeaders managerHeaders = headersFor("demo-manager", HrAnalyticsAuthorities.NOMINAL_READ);

        ResponseEntity<String> mixedIncludeIds = restTemplate.postForEntity(
                filterPath + "&includeIds=cur-ops-1&includeIds=cur-hr-1",
                new HttpEntity<>("{\"criticalityLevel\":\"CRITICAL\"}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, mixedIncludeIds.getStatusCode(), mixedIncludeIds.getBody());
        assertEquals(java.util.Set.of("cur-ops-1", "cur-ops-2"), resourceIds(mixedIncludeIds, "analyticsId"));

        ResponseEntity<String> noIncludeIds = restTemplate.postForEntity(
                filterPath,
                new HttpEntity<>("{\"criticalityLevel\":\"CRITICAL\"}", managerHeaders),
                String.class);
        assertEquals(java.util.Set.of("cur-ops-2"), resourceIds(noIncludeIds, "analyticsId"));

        ResponseEntity<String> emptyIncludeIds = restTemplate.postForEntity(
                filterPath + "&includeIds=",
                new HttpEntity<>("{\"criticalityLevel\":\"CRITICAL\"}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, emptyIncludeIds.getStatusCode(), emptyIncludeIds.getBody());
        assertEquals(java.util.Set.of("cur-ops-2"), resourceIds(emptyIncludeIds, "analyticsId"));

        ResponseEntity<String> denied = restTemplate.postForEntity(
                filterPath + "&includeIds=cur-ops-1",
                new HttpEntity<>("{}", headersFor("blocked", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertSafeForbidden(denied);

        ResponseEntity<String> aggregateOnly = restTemplate.postForEntity(
                filterPath + "&includeIds=cur-ops-1",
                new HttpEntity<>("{}", headersFor("aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ)),
                String.class);
        assertSafeForbidden(aggregateOnly);

        ResponseEntity<String> global = restTemplate.postForEntity(
                filterPath,
                new HttpEntity<>("{}", headersFor("global", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertEquals(HttpStatus.OK, global.getStatusCode(), global.getBody());
        assertEquals(5, resourceIds(global, "analyticsId").size());
    }

    @Test
    void shouldPublishPrincipalAwareCapabilitiesAndCanonicalRecordOpenTarget() throws Exception {
        JsonNode aggregateCapabilities = getJson(
                "/api/human-resources/vw-analytics-afastamentos/capabilities",
                headersFor("aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ)
        );
        assertOperationAvailability(aggregateCapabilities, "statsComparison", true, null);
        assertOperationAvailability(aggregateCapabilities, "filter", false, "missing-authority");
        assertOperationAvailability(aggregateCapabilities, "cursor", false, "missing-authority");

        JsonNode nominalCapabilities = getJson(
                "/api/human-resources/vw-analytics-afastamentos/capabilities",
                headersFor("nominal-reader", HrAnalyticsAuthorities.NOMINAL_READ)
        );
        assertOperationAvailability(nominalCapabilities, "filter", true, null);
        assertOperationAvailability(nominalCapabilities, "cursor", true, null);
        assertOperationAvailability(nominalCapabilities, "statsComparison", false, "missing-authority");

        JsonNode aggregateSurfaceCatalog = getJson(
                "/schemas/surfaces?resource=human-resources.funcionarios",
                headersFor("aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ)
        );
        JsonNode deniedTarget = findById(aggregateSurfaceCatalog.path("surfaces"), "hero-profile");
        assertNotNull(deniedTarget);
        assertFalse(deniedTarget.path("availability").path("allowed").asBoolean());
        assertEquals("missing-authority", deniedTarget.path("availability").path("reason").asText());

        JsonNode eligibleSurfaceCatalog = getJson(
                "/schemas/surfaces?resource=human-resources.funcionarios",
                headersFor("employee-360", HrAnalyticsAuthorities.EMPLOYEE_360_READ)
        );
        JsonNode eligibleTarget = findById(eligibleSurfaceCatalog.path("surfaces"), "hero-profile");
        assertNotNull(eligibleTarget);
        assertFalse(eligibleTarget.path("availability").path("allowed").asBoolean());
        assertEquals("resource-context-required", eligibleTarget.path("availability").path("reason").asText());
    }

    @Test
    void shouldRejectEmployeeFilterForAggregateOnlyComparison() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                "aggregate-only", "USER", java.util.List.of(HrAnalyticsAuthorities.AGGREGATE_READ)
        ));
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "filter": { "funcionarioIdsIn": [102] },
                  "field": "departamento",
                  "periodField": "competencia",
                  "period": {
                    "from": "2026-07-01",
                    "to": "2026-07-31",
                    "timezone": "America/Sao_Paulo",
                    "mode": "PREVIOUS_ALIGNED"
                  },
                  "metrics": [
                    { "operation": "DISTINCT_COUNT", "field": "funcionarioId", "alias": "colaboradores" },
                    { "operation": "SUM", "field": "diasAfastado", "alias": "dias" }
                  ]
                }
                """, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/vw-analytics-afastamentos/stats/comparison",
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldApplyDepartmentScopeToEveryInheritedStatsOperation() throws Exception {
        HttpHeaders headers = headersFor("demo-manager", HrAnalyticsAuthorities.NOMINAL_READ);

        assertScopedStatsResponse(
                "/api/human-resources/vw-analytics-afastamentos/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "departamento",
                          "metric": { "operation": "COUNT", "alias": "linhas" }
                        }
                        """,
                """
                        {
                          "filter": { "departamentoIdsIn": [2] },
                          "field": "departamento",
                          "metric": { "operation": "COUNT", "alias": "linhas" }
                        }
                        """,
                headers,
                response -> assertEquals(1, response.path("buckets").size())
        );

        assertScopedStatsResponse(
                "/api/human-resources/vw-analytics-afastamentos/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "competencia",
                          "granularity": "MONTH",
                          "metric": { "operation": "SUM", "field": "diasAfastado", "alias": "dias" }
                        }
                        """,
                """
                        {
                          "filter": { "departamentoIdsIn": [2] },
                          "field": "competencia",
                          "granularity": "MONTH",
                          "metric": { "operation": "SUM", "field": "diasAfastado", "alias": "dias" }
                        }
                        """,
                headers,
                response -> assertEquals(2, response.path("points").size())
        );

        assertScopedStatsResponse(
                "/api/human-resources/vw-analytics-afastamentos/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "criticalityLevel",
                          "mode": "TERMS",
                          "metric": { "operation": "COUNT", "alias": "linhas" }
                        }
                        """,
                """
                        {
                          "filter": { "departamentoIdsIn": [2] },
                          "field": "criticalityLevel",
                          "mode": "TERMS",
                          "metric": { "operation": "COUNT", "alias": "linhas" }
                        }
                        """,
                headers,
                response -> assertEquals(3, response.path("buckets").size())
        );
    }

    @Test
    void shouldApplyDepartmentScopeToAllCriticalityOptionSourcePaths() throws Exception {
        HttpHeaders managerHeaders = headersFor("demo-manager", HrAnalyticsAuthorities.NOMINAL_READ);
        String path = "/api/human-resources/vw-analytics-afastamentos/option-sources/criticalityLevel/options";

        ResponseEntity<String> autoScoped = restTemplate.postForEntity(
                path + "/filter?page=0&size=25", new HttpEntity<>("{}", managerHeaders), String.class);
        assertEquals(HttpStatus.OK, autoScoped.getStatusCode(), autoScoped.getBody());
        assertFalse(optionIds(autoScoped).contains("OUTSIDE_SCOPE_ONLY"), autoScoped.getBody());
        assertTrue(optionIds(autoScoped).contains("CRITICAL"), autoScoped.getBody());

        ResponseEntity<String> partialScope = restTemplate.postForEntity(
                path + "/filter?page=0&size=25",
                new HttpEntity<>("{\"filter\":{\"departamentoIdsIn\":[1,2]}}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, partialScope.getStatusCode(), partialScope.getBody());
        assertFalse(optionIds(partialScope).contains("OUTSIDE_SCOPE_ONLY"), partialScope.getBody());

        ResponseEntity<String> includedOutsideScope = restTemplate.postForEntity(
                path + "/filter?page=0&size=25&includeIds=OUTSIDE_SCOPE_ONLY",
                new HttpEntity<>("{}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, includedOutsideScope.getStatusCode(), includedOutsideScope.getBody());
        assertFalse(optionIds(includedOutsideScope).contains("OUTSIDE_SCOPE_ONLY"), includedOutsideScope.getBody());

        ResponseEntity<String> outsideScope = restTemplate.postForEntity(
                path + "/filter?page=0&size=25",
                new HttpEntity<>("{\"filter\":{\"departamentoIdsIn\":[2]}}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, outsideScope.getStatusCode(), outsideScope.getBody());

        ResponseEntity<String> contextualOutsideScope = restTemplate.postForEntity(
                path + "/by-ids",
                new HttpEntity<>("{\"filter\":{\"departamentoIdsIn\":[2]},\"ids\":[\"OUTSIDE_SCOPE_ONLY\"]}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, contextualOutsideScope.getStatusCode(), contextualOutsideScope.getBody());

        ResponseEntity<String> getByIds = restTemplate.exchange(
                path + "/by-ids?ids=OUTSIDE_SCOPE_ONLY&ids=CRITICAL",
                HttpMethod.GET,
                new HttpEntity<>(managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, getByIds.getStatusCode(), getByIds.getBody());
        assertFalse(getByIds.getBody().contains("OUTSIDE_SCOPE_ONLY"), getByIds.getBody());
        assertTrue(getByIds.getBody().contains("CRITICAL"), getByIds.getBody());

        ResponseEntity<String> emptyGetByIds = restTemplate.exchange(
                path + "/by-ids",
                HttpMethod.GET,
                new HttpEntity<>(headersFor("blocked", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, emptyGetByIds.getStatusCode(), emptyGetByIds.getBody());

        ResponseEntity<String> emptyPostByIds = restTemplate.postForEntity(
                path + "/by-ids",
                new HttpEntity<>("{\"ids\":[]}", headersFor("blocked", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, emptyPostByIds.getStatusCode(), emptyPostByIds.getBody());

        ResponseEntity<String> aggregateOnly = restTemplate.postForEntity(
                path + "/filter?page=0&size=25",
                new HttpEntity<>("{}", headersFor("aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, aggregateOnly.getStatusCode(), aggregateOnly.getBody());

        ResponseEntity<String> emptyScope = restTemplate.postForEntity(
                path + "/filter?page=0&size=25",
                new HttpEntity<>("{}", headersFor("blocked", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, emptyScope.getStatusCode(), emptyScope.getBody());
    }

    private void assertScopedStatsResponse(
            String path,
            String unfilteredPayload,
            String outsideScopePayload,
            HttpHeaders headers,
            JsonNodeAssertion unfilteredAssertion
    ) throws Exception {
        JsonNode unfiltered = postJson(path, unfilteredPayload, headers);
        unfilteredAssertion.assertThat(unfiltered);
        ResponseEntity<String> outsideScope = restTemplate.postForEntity(
                path,
                new HttpEntity<>(outsideScopePayload, headers),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, outsideScope.getStatusCode(), outsideScope.getBody());
    }

    private JsonNode postJson(String path, String body, HttpHeaders headers) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals("success", json.path("status").asText(), response.getBody());
        return json.path("data");
    }

    private java.util.Set<String> optionIds(ResponseEntity<String> response) throws Exception {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        for (JsonNode option : objectMapper.readTree(response.getBody()).path("content")) {
            ids.add(option.path("id").asText());
        }
        return ids;
    }

    private java.util.Set<String> resourceIds(ResponseEntity<String> response, String idField) throws Exception {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        for (JsonNode row : objectMapper.readTree(response.getBody()).path("data").path("content")) {
            ids.add(row.path(idField).asText());
        }
        return ids;
    }

    private void assertSafeForbidden(ResponseEntity<String> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), response.getBody());
        assertFalse(response.getBody() != null && response.getBody().contains("java."), response.getBody());
        assertFalse(response.getBody() != null && response.getBody().contains("at com."), response.getBody());
    }

    private JsonNode getJson(String path, HttpHeaders headers) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private void assertOperationAvailability(
            JsonNode capabilities,
            String operationId,
            boolean allowed,
            String reason
    ) {
        JsonNode operation = capabilities.path("operations").path(operationId);
        assertTrue(operation.path("supported").asBoolean(), operationId);
        assertEquals(allowed, operation.path("availability").path("allowed").asBoolean(), operationId);
        if (reason == null) {
            JsonNode publishedReason = operation.path("availability").path("reason");
            assertTrue(publishedReason.isMissingNode() || publishedReason.isNull(), operationId);
        } else {
            assertEquals(reason, operation.path("availability").path("reason").asText(), operationId);
        }
        assertEquals("hr-analytics-access", operation.path("availability").path("metadata").path("policy").asText());
    }

    private HttpHeaders headersFor(String subject, String... authorities) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(subject, "USER", java.util.List.of(authorities)));
        return headers;
    }

    @FunctionalInterface
    private interface JsonNodeAssertion {
        void assertThat(JsonNode response);
    }

    private JsonNode findBucket(JsonNode buckets, int key) {
        for (JsonNode bucket : buckets) {
            if (bucket.path("key").asInt() == key) {
                return bucket;
            }
        }
        return null;
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText())) {
                return item;
            }
        }
        return null;
    }
}
