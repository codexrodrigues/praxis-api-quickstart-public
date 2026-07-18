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

import java.util.List;

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
                "app.hr.analytics.demo-department-scopes=global=*,aggregate-only=*,manager=1|2,blocked=",
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_payroll_comparison;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_payroll_comparison_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class PayrollAnalyticsComparisonHttpTest {
    private static final String RESOURCE = "/api/human-resources/vw-analytics-folha-pagamento";

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
    void seedView() {
        jdbcTemplate.execute("drop table if exists public.vw_analytics_folha_pagamento");
        jdbcTemplate.execute("""
                create table public.vw_analytics_folha_pagamento (
                    folha_pagamento_id integer primary key, funcionario_id integer,
                    nome_completo varchar(200), codinome varchar(200), universo varchar(200), exposicao_publica boolean,
                    cargo_id integer, cargo varchar(200), departamento_id integer, departamento varchar(200),
                    equipe_id integer, equipe varchar(200), papel_equipe varchar(200), base_id integer,
                    base varchar(200), tipo_base varchar(200), sigilo_base varchar(200), ano integer, mes integer,
                    competencia date, data_pagamento date, salario_bruto numeric(19,2), total_descontos numeric(19,2),
                    salario_liquido numeric(19,2), qtd_eventos bigint, qtd_proventos bigint, qtd_descontos bigint,
                    qtd_adicionais bigint, qtd_tipos_evento bigint, valor_proventos numeric(19,2),
                    valor_descontos_eventos numeric(19,2), valor_adicionais numeric(19,2), saldo_eventos numeric(19,2),
                    saldo_liquido_vs_bruto numeric(19,2), pct_desconto numeric(19,4), pct_liquido numeric(19,4),
                    pct_adicionais_sobre_bruto numeric(19,4), pct_eventos_desconto_sobre_bruto numeric(19,4),
                    faixa_salario_bruto varchar(200), faixa_salario_liquido varchar(200), faixa_pct_desconto varchar(200),
                    faixa_valor_adicionais varchar(200), payroll_profile varchar(200), composicao_folha varchar(200),
                    eventos_descricao varchar(500)
                )
                """);
        jdbcTemplate.update("""
                insert into public.vw_analytics_folha_pagamento
                    (folha_pagamento_id, funcionario_id, nome_completo, departamento_id, departamento,
                     ano, mes, competencia, salario_bruto, total_descontos, salario_liquido)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 101, 1, "Pessoa A", 1, "Operacoes Legadas", 2026, 6, "2026-06-01", 1000, 100, 900);
        insert(102, 2, 1, "Operacoes Legadas", "2026-06-01", 2000, 200, 1800);
        insert(103, 3, 3, "Projeto Encerrado", "2026-06-01", 3000, 300, 2700);
        insert(201, 1, 2, "Operacoes Atuais", "2026-07-01", 1100, 110, 990);
        insert(202, 2, 1, "Operacoes Legadas", "2026-07-01", 2100, 210, 1890);
        insert(204, 4, 4, "Nova Unidade", "2026-07-01", 4000, 400, 3600);
    }

    @Test
    void shouldCompareExactPayrollMassWithStableKeysAndZeroFilledPeriods() throws Exception {
        ResponseEntity<String> response = compare("global", List.of(HrAnalyticsAuthorities.AGGREGATE_READ), "{}");

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertBucket(data.path("buckets"), 1, "Operacoes Legadas", 2100, 3000);
        assertBucket(data.path("buckets"), 2, "Operacoes Atuais", 1100, 0);
        assertBucket(data.path("buckets"), 3, "Projeto Encerrado", 0, 3000);
        assertBucket(data.path("buckets"), 4, "Nova Unidade", 4000, 0);
        assertEquals(3, data.path("metrics").size());
        assertFalse(response.getBody().contains("funcionarioId"));
        assertFalse(response.getBody().contains("nomeCompleto"));
    }

    @Test
    void shouldReapplyBucketKeyAndIntersectManagerScope() throws Exception {
        ResponseEntity<String> manager = compare(
                "manager",
                List.of(HrAnalyticsAuthorities.AGGREGATE_READ, HrAnalyticsAuthorities.NOMINAL_READ),
                "{\"departamentoId\":2}"
        );

        assertEquals(HttpStatus.OK, manager.getStatusCode(), manager.getBody());
        JsonNode buckets = objectMapper.readTree(manager.getBody()).path("data").path("buckets");
        assertEquals(1, buckets.size());
        assertBucket(buckets, 2, "Operacoes Atuais", 1100, 0);

        ResponseEntity<String> outsideScope = compare(
                "manager",
                List.of(HrAnalyticsAuthorities.AGGREGATE_READ, HrAnalyticsAuthorities.NOMINAL_READ),
                "{\"departamentoId\":4}"
        );
        assertEquals(HttpStatus.FORBIDDEN, outsideScope.getStatusCode());
    }

    @Test
    void shouldApplyTheSamePrincipalScopeToInheritedFilterAndGroupByOperations() throws Exception {
        HttpHeaders headers = headersFor(
                "manager", HrAnalyticsAuthorities.AGGREGATE_READ, HrAnalyticsAuthorities.NOMINAL_READ);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> rows = restTemplate.exchange(
                RESOURCE + "/filter?page=0&size=20",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class);
        assertEquals(HttpStatus.OK, rows.getStatusCode(), rows.getBody());
        JsonNode rowContent = objectMapper.readTree(rows.getBody()).path("data").path("content");
        assertEquals(4, rowContent.size());
        for (JsonNode row : rowContent) {
            assertTrue(row.path("departamentoId").asInt() == 1 || row.path("departamentoId").asInt() == 2);
        }

        String groupByBody = """
                {
                  "filter": {},
                  "field": "departamento",
                  "metric": { "operation": "SUM", "field": "salarioBruto", "alias": "bruto" },
                  "limit": 20,
                  "orderBy": "KEY_ASC"
                }
                """;
        ResponseEntity<String> grouped = restTemplate.postForEntity(
                RESOURCE + "/stats/group-by", new HttpEntity<>(groupByBody, headers), String.class);
        assertEquals(HttpStatus.OK, grouped.getStatusCode(), grouped.getBody());
        JsonNode buckets = objectMapper.readTree(grouped.getBody()).path("data").path("buckets");
        assertEquals(2, buckets.size());
        assertNotNull(findByKey(buckets, 1));
        assertNotNull(findByKey(buckets, 2));
    }

    @Test
    void shouldKeepIncludedPayrollRowsInsideTheServerResolvedResourceScope() throws Exception {
        String filterPath = RESOURCE + "/filter?page=0&size=20";
        HttpHeaders managerHeaders = headersFor(
                "manager", HrAnalyticsAuthorities.AGGREGATE_READ, HrAnalyticsAuthorities.NOMINAL_READ);
        managerHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> mixedIncludeIds = restTemplate.postForEntity(
                filterPath + "&includeIds=201&includeIds=103",
                new HttpEntity<>("{\"departamentoId\":1}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, mixedIncludeIds.getStatusCode(), mixedIncludeIds.getBody());
        assertEquals(java.util.Set.of(101, 102, 201, 202), payrollIds(mixedIncludeIds));

        ResponseEntity<String> noIncludeIds = restTemplate.postForEntity(
                filterPath,
                new HttpEntity<>("{\"departamentoId\":1}", managerHeaders),
                String.class);
        assertEquals(java.util.Set.of(101, 102, 202), payrollIds(noIncludeIds));

        ResponseEntity<String> emptyIncludeIds = restTemplate.postForEntity(
                filterPath + "&includeIds=",
                new HttpEntity<>("{\"departamentoId\":1}", managerHeaders),
                String.class);
        assertEquals(HttpStatus.OK, emptyIncludeIds.getStatusCode(), emptyIncludeIds.getBody());
        assertEquals(java.util.Set.of(101, 102, 202), payrollIds(emptyIncludeIds));

        ResponseEntity<String> denied = restTemplate.postForEntity(
                filterPath + "&includeIds=101",
                new HttpEntity<>("{}", headersFor("blocked", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertSafeForbidden(denied);

        ResponseEntity<String> aggregateOnly = restTemplate.postForEntity(
                filterPath + "&includeIds=101",
                new HttpEntity<>("{}", headersFor("aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ)),
                String.class);
        assertSafeForbidden(aggregateOnly);

        ResponseEntity<String> global = restTemplate.postForEntity(
                filterPath,
                new HttpEntity<>("{}", headersFor("global", HrAnalyticsAuthorities.NOMINAL_READ)),
                String.class);
        assertEquals(HttpStatus.OK, global.getStatusCode(), global.getBody());
        assertEquals(6, payrollIds(global).size());
    }

    @Test
    void shouldFailClosedWithoutScopeAndKeepAggregateOnlyAwayFromNominalRows() {
        ResponseEntity<String> blocked = compare(
                "blocked", List.of(HrAnalyticsAuthorities.AGGREGATE_READ), "{}");
        assertEquals(HttpStatus.FORBIDDEN, blocked.getStatusCode());

        HttpEntity<Void> nominalRequest = new HttpEntity<>(headersFor(
                "aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ));
        ResponseEntity<String> nominal = restTemplate.exchange(
                RESOURCE + "/101", HttpMethod.GET, nominalRequest, String.class);
        assertEquals(HttpStatus.FORBIDDEN, nominal.getStatusCode());
    }

    @Test
    void shouldPublishPrincipalAwareCapabilitiesForPayrollComparison() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(headersFor(
                "aggregate-only", HrAnalyticsAuthorities.AGGREGATE_READ));
        ResponseEntity<String> response = restTemplate.exchange(
                RESOURCE + "/capabilities", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode operations = objectMapper.readTree(response.getBody()).path("operations");
        assertAvailability(operations, "statsComparison", true);
        assertAvailability(operations, "filter", false);
    }

    @Test
    void shouldPublishCanonicalComparisonInOpenApiAndAnalyticsMetadata() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/v3/api-docs/api-human-resources-vw-analytics-folha-pagamento", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertTrue(response.getBody().contains(RESOURCE + "/stats/comparison"));
        assertTrue(response.getBody().contains("payroll-department-comparison"));
        assertTrue(response.getBody().contains("departamentoId"));
        assertTrue(response.getBody().contains("salarioBruto"));
        assertTrue(response.getBody().contains("salarioLiquido"));
    }

    private ResponseEntity<String> compare(String subject, List<String> authorities, String filter) {
        HttpHeaders headers = headersFor(subject, authorities.toArray(String[]::new));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "filter": %s,
                  "field": "departamento",
                  "periodField": "competencia",
                  "period": {
                    "from": "2026-07-01",
                    "to": "2026-07-31",
                    "timezone": "America/Sao_Paulo",
                    "mode": "PREVIOUS_ALIGNED"
                  },
                  "metrics": [
                    { "operation": "SUM", "field": "salarioBruto", "alias": "bruto" },
                    { "operation": "SUM", "field": "totalDescontos", "alias": "descontos" },
                    { "operation": "SUM", "field": "salarioLiquido", "alias": "liquido" }
                  ],
                  "orderBy": "KEY_ASC",
                  "limit": 20
                }
                """.formatted(filter);
        return restTemplate.postForEntity(RESOURCE + "/stats/comparison", new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders headersFor(String subject, String... authorities) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                subject, "USER", List.of(authorities)));
        return headers;
    }

    private void insert(
            int payrollId, int employeeId, int departmentId, String department, String competence,
            int gross, int discounts, int net
    ) {
        jdbcTemplate.update("""
                insert into public.vw_analytics_folha_pagamento
                    (folha_pagamento_id, funcionario_id, nome_completo, departamento_id, departamento,
                     ano, mes, competencia, salario_bruto, total_descontos, salario_liquido)
                values (?, ?, ?, ?, ?, 2026, ?, ?, ?, ?, ?)
                """, payrollId, employeeId, "Pessoa " + employeeId, departmentId, department,
                competence.endsWith("06-01") ? 6 : 7, competence, gross, discounts, net);
    }

    private void assertBucket(JsonNode buckets, int key, String label, int currentGross, int previousGross) {
        JsonNode bucket = findByKey(buckets, key);
        assertNotNull(bucket, "Missing department bucket " + key);
        assertEquals(label, bucket.path("label").asText());
        assertEquals(currentGross, bucket.path("values").path("bruto").path("current").asInt());
        assertEquals(previousGross, bucket.path("values").path("bruto").path("previous").asInt());
        assertTrue(bucket.path("values").path("descontos").has("delta"));
        assertTrue(bucket.path("values").path("liquido").has("delta"));
    }

    private JsonNode findByKey(JsonNode buckets, int key) {
        for (JsonNode bucket : buckets) {
            if (bucket.path("key").asInt() == key) {
                return bucket;
            }
        }
        return null;
    }

    private java.util.Set<Integer> payrollIds(ResponseEntity<String> response) throws Exception {
        java.util.Set<Integer> ids = new java.util.LinkedHashSet<>();
        for (JsonNode row : objectMapper.readTree(response.getBody()).path("data").path("content")) {
            ids.add(row.path("folhaPagamentoId").asInt());
        }
        return ids;
    }

    private void assertSafeForbidden(ResponseEntity<String> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), response.getBody());
        assertFalse(response.getBody() != null && response.getBody().contains("java."), response.getBody());
        assertFalse(response.getBody() != null && response.getBody().contains("at com."), response.getBody());
    }

    private void assertAvailability(JsonNode operations, String operationId, boolean allowed) {
        JsonNode operation = operations.path(operationId);
        assertTrue(operation.path("supported").asBoolean(), "Missing capability operation " + operationId);
        assertEquals(allowed, operation.path("availability").path("allowed").asBoolean(), operationId);
        assertEquals("hr-analytics-access",
                operation.path("availability").path("metadata").path("policy").asText(), operationId);
    }
}
