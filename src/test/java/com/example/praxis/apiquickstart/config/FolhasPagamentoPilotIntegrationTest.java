package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_folhas_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_folhas_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class FolhasPagamentoPilotIntegrationTest {

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

    @MockBean
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @BeforeEach
    void seedTables() {
        LocalDate today = LocalDate.now();
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.eventos_folha");
        jdbcTemplate.execute("drop table if exists public.folhas_pagamento");
        jdbcTemplate.execute("drop table if exists public.funcionarios");

        jdbcTemplate.execute("""
                create table public.funcionarios (
                    id integer primary key,
                    version bigint not null default 0,
                    nome_completo varchar(200) not null,
                    cpf varchar(11) not null,
                    data_nascimento date not null,
                    email varchar(255) not null,
                    telefone varchar(30) not null,
                    salario decimal(18,2) not null,
                    data_admissao date not null,
                    ativo boolean not null,
                    cargo_id integer not null,
                    departamento_id integer not null,
                    foto_perfil_url varchar(400),
                    estado_civil varchar(40),
                    pais_nascimento varchar(120),
                    cidade_nascimento varchar(120)
                )
                """);

        jdbcTemplate.execute("""
                create table public.folhas_pagamento (
                    id integer primary key,
                    ano integer not null,
                    mes integer not null,
                    salario_bruto decimal(18,2) not null,
                    total_descontos decimal(18,2) not null,
                    salario_liquido decimal(18,2) not null,
                    data_pagamento date not null,
                    funcionario_id integer not null
                )
                """);

        jdbcTemplate.execute("""
                create table public.eventos_folha (
                    id integer primary key,
                    descricao varchar(255) not null,
                    tipo varchar(255) not null,
                    valor decimal(18,2) not null,
                    folha_pagamento_id integer not null,
                    status varchar(20) not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (1, 'Carol Danvers', '00000000011', DATE '1985-01-15', 'carol.danvers@shield.org', '+55-11-99999-0111', 25000.00, DATE '2021-01-10', true, 1, 1, null, 'SOLTEIRO', 'Estados Unidos', 'Boston')
                """);

        jdbcTemplate.update("""
                insert into public.folhas_pagamento (id, ano, mes, salario_bruto, total_descontos, salario_liquido, data_pagamento, funcionario_id)
                values (1, 2026, 4, 25000.00, 1200.00, 23800.00, ?, 1)
                """, java.sql.Date.valueOf(today.plusDays(10)));
        jdbcTemplate.update("""
                insert into public.folhas_pagamento (id, ano, mes, salario_bruto, total_descontos, salario_liquido, data_pagamento, funcionario_id)
                values (2, 2026, 3, 25000.00, 900.00, 24100.00, ?, 1)
                """, java.sql.Date.valueOf(today.plusDays(5)));
        jdbcTemplate.update("""
                insert into public.folhas_pagamento (id, ano, mes, salario_bruto, total_descontos, salario_liquido, data_pagamento, funcionario_id)
                values (3, 2026, 2, 25000.00, 800.00, 24200.00, ?, 1)
                """, java.sql.Date.valueOf(today.minusDays(2)));

        jdbcTemplate.update("""
                insert into public.eventos_folha (id, descricao, tipo, valor, folha_pagamento_id, status)
                values (1, 'Hazard allowance', 'CREDITO', 600.00, 1, 'PENDENTE')
                """);
        jdbcTemplate.update("""
                insert into public.eventos_folha (id, descricao, tipo, valor, folha_pagamento_id, status)
                values (2, 'Overtime', 'CREDITO', 350.00, 1, 'PENDENTE')
                """);
        jdbcTemplate.update("""
                insert into public.eventos_folha (id, descricao, tipo, valor, folha_pagamento_id, status)
                values (3, 'Insurance discount', 'DEBITO', 120.00, 2, 'APROVADO')
                """);
        jdbcTemplate.update("""
                insert into public.eventos_folha (id, descricao, tipo, valor, folha_pagamento_id, status)
                values (4, 'Meal voucher', 'CREDITO', 300.00, 3, 'APROVADO')
                """);
    }

    @Test
    void shouldExposePayrollSurfaceAndWorkflowActions() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.folhas-pagamento",
                String.class
        ));
        JsonNode paymentSchedule = findById(surfacesCatalog.path("surfaces"), "payment-schedule");
        assertNotNull(paymentSchedule);
        assertEquals("/api/human-resources/folhas-pagamento/{id}/payment-schedule", paymentSchedule.path("path").asText());

        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=human-resources.folhas-pagamento",
                String.class
        ));
        assertNotNull(findById(actionsCatalog.path("actions"), "approve-events"));
        assertNotNull(findById(actionsCatalog.path("actions"), "mark-paid"));

        JsonNode scheduleRequestSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=patch&schemaType=request",
                String.class,
                "/api/human-resources/folhas-pagamento/{id}/payment-schedule"
        ));
        JsonNode paymentDateUi = scheduleRequestSchema.path("properties").path("dataPagamento").path("x-ui");
        assertEquals("Data de pagamento", paymentDateUi.path("label").asText());
        assertEquals("Data programada para execução da folha.", paymentDateUi.path("helpText").asText());

        JsonNode approveEventsRequestSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                "/api/human-resources/folhas-pagamento/{id}/actions/approve-events"
        ));
        assertEquals("Justificativa",
                approveEventsRequestSchema.path("properties").path("justificativa").path("x-ui").path("label").asText());

        JsonNode awaitingCapabilities = body(restTemplate.getForEntity(
                "/api/human-resources/folhas-pagamento/1/capabilities",
                String.class
        ));
        assertTrue(findById(awaitingCapabilities.path("surfaces"), "payment-schedule").path("availability").path("allowed").asBoolean());
        assertTrue(findById(awaitingCapabilities.path("actions"), "approve-events").path("availability").path("allowed").asBoolean());
        assertFalse(findById(awaitingCapabilities.path("actions"), "mark-paid").path("availability").path("allowed").asBoolean());

        JsonNode scheduledActions = body(restTemplate.getForEntity(
                "/api/human-resources/folhas-pagamento/2/actions",
                String.class
        ));
        assertFalse(findById(scheduledActions.path("actions"), "approve-events").path("availability").path("allowed").asBoolean());
        assertTrue(findById(scheduledActions.path("actions"), "mark-paid").path("availability").path("allowed").asBoolean());

        JsonNode paidCapabilities = body(restTemplate.getForEntity(
                "/api/human-resources/folhas-pagamento/3/capabilities",
                String.class
        ));
        assertFalse(findById(paidCapabilities.path("surfaces"), "payment-schedule").path("availability").path("allowed").asBoolean());
        assertFalse(findById(paidCapabilities.path("actions"), "mark-paid").path("availability").path("allowed").asBoolean());
    }

    @Test
    void shouldScheduleApproveEventsAndMarkPayrollAsPaid() throws Exception {
        LocalDate today = LocalDate.now();

        ResponseEntity<String> rescheduleResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/2/payment-schedule",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "dataPagamento": "%s"
                        }
                        """.formatted(today.plusDays(12))),
                String.class
        );
        JsonNode rescheduleBody = body(rescheduleResponse);
        assertEquals(today.plusDays(12).toString(), rescheduleBody.path("data").path("dataPagamento").asText());

        ResponseEntity<String> approveEventsResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/1/actions/approve-events",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Payroll review completed"
                        }
                        """),
                String.class
        );
        JsonNode approveBody = body(approveEventsResponse);
        assertEquals("AGUARDANDO_EVENTOS", approveBody.path("data").path("estadoAnterior").asText());
        assertEquals("PROGRAMADA", approveBody.path("data").path("estadoAtual").asText());
        assertEquals(2, approveBody.path("data").path("eventosProcessados").asInt());
        assertFalse(approveBody.path("_links").path("schema").isMissingNode(), approveBody.toPrettyString());
        assertEquals(0L, jdbcTemplate.queryForObject(
                "select count(*) from public.eventos_folha where folha_pagamento_id = 1 and status = 'PENDENTE'",
                Long.class
        ));

        ResponseEntity<String> duplicateApproveResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/1/actions/approve-events",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Duplicate"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateApproveResponse.getStatusCode());
        assertNotNull(duplicateApproveResponse.getBody());
        JsonNode duplicateApproveBody = objectMapper.readTree(duplicateApproveResponse.getBody());
        assertEquals("failure", duplicateApproveBody.path("status").asText());
        assertEquals("CONFLICT_DEPENDENCY", duplicateApproveBody.path("errors").get(0).path("outcome").asText());

        ResponseEntity<String> markPaidResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/2/actions/mark-paid",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Treasury release completed"
                        }
                        """),
                String.class
        );
        JsonNode markPaidBody = body(markPaidResponse);
        assertEquals("PROGRAMADA", markPaidBody.path("data").path("estadoAnterior").asText());
        assertEquals("PAGA", markPaidBody.path("data").path("estadoAtual").asText());
        assertFalse(markPaidBody.path("_links").path("schema").isMissingNode(), markPaidBody.toPrettyString());
        assertEquals(today.toString(), jdbcTemplate.queryForObject(
                "select cast(data_pagamento as varchar) from public.folhas_pagamento where id = 2",
                String.class
        ));
    }

    @Test
    void shouldBlockMarkPaidWhenGovernedWorkflowActionPolicyIsApplied() {
        LocalDate today = LocalDate.now();
        when(workflowActionPolicyResolver.resolveAppliedPolicy("human-resources.folhas-pagamento:mark-paid"))
                .thenReturn(Optional.of(new DomainRuleWorkflowActionPolicy(
                        "human-resources.folhas-pagamento:mark-paid",
                        "human-resources.folhas-pagamento",
                        "mark-paid",
                        List.of("PROGRAMADA"),
                        "Pagamento bloqueado por decisao governada ate revisao de compliance."
                )));

        ResponseEntity<String> markPaidResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/2/actions/mark-paid",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Treasury release completed"
                        }
                        """),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, markPaidResponse.getStatusCode());
        assertNotNull(markPaidResponse.getBody());
        JsonNode markPaidBody;
        try {
            markPaidBody = objectMapper.readTree(markPaidResponse.getBody());
        } catch (Exception ex) {
            throw new AssertionError("Expected governed failure body", ex);
        }
        assertEquals("failure", markPaidBody.path("status").asText());
        assertEquals("CONFLICT_DEPENDENCY", markPaidBody.path("errors").get(0).path("outcome").asText());
        assertEquals(today.plusDays(5).toString(), jdbcTemplate.queryForObject(
                "select cast(data_pagamento as varchar) from public.folhas_pagamento where id = 2",
                String.class
        ));
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
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
