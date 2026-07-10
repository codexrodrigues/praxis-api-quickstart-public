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
                "spring.datasource.url=jdbc:h2:mem:quickstart_absences_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_absences_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class FeriasAfastamentoPilotIntegrationTest {

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
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.ferias_afastamentos");
        jdbcTemplate.execute("drop table if exists public.funcionarios");

        jdbcTemplate.execute("""
                create table public.funcionarios (
                    id integer primary key,
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
                create table public.ferias_afastamentos (
                    id integer primary key,
                    tipo varchar(40) not null,
                    data_inicio date not null,
                    data_fim date not null,
                    observacoes varchar(2000),
                    funcionario_id integer not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (1, 'Diana Prince', '00000000021', DATE '1984-03-22', 'diana.prince@themyscira.org', '+55-11-99999-0021', 30000.00, DATE '2020-02-01', true, 1, 1, null, 'SOLTEIRO', 'Themyscira', 'Themyscira')
                """);

        jdbcTemplate.update("""
                insert into public.ferias_afastamentos (id, tipo, data_inicio, data_fim, observacoes, funcionario_id)
                values (1, 'FERIAS', DATE '2026-07-01', DATE '2026-07-15', 'Aprovado por RH.', 1)
                """);
    }

    @Test
    void shouldExposeAndExecuteGovernedAbsenceCoverageAction() throws Exception {
        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=human-resources.ferias-afastamentos",
                String.class
        ));
        JsonNode planCoverage = findById(actionsCatalog.path("actions"), "plan-coverage");
        assertNotNull(planCoverage);
        assertEquals("/api/human-resources/ferias-afastamentos/{id}/actions/plan-coverage",
                planCoverage.path("path").asText());

        JsonNode requestSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                "/api/human-resources/ferias-afastamentos/{id}/actions/plan-coverage"
        ));
        assertEquals("Plano de cobertura",
                requestSchema.path("properties").path("planoCobertura").path("x-ui").path("label").asText());

        JsonNode capabilities = body(restTemplate.getForEntity(
                "/api/human-resources/ferias-afastamentos/1/capabilities",
                String.class
        ));
        assertTrue(findById(capabilities.path("actions"), "plan-coverage").path("availability").path("allowed").asBoolean());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/human-resources/ferias-afastamentos/1/actions/plan-coverage",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "planoCobertura": "Cobertura pela equipe B.",
                          "substitutoFuncionarioId": 1,
                          "justificativa": "Ausencia cruza fechamento operacional."
                        }
                        """),
                String.class
        );

        JsonNode body = body(response);
        assertEquals("Cobertura pela equipe B.", body.path("data").path("planoCobertura").asText());
        assertEquals(1, body.path("data").path("substitutoFuncionarioId").asInt());
        assertFalse(body.path("_links").path("schema").isMissingNode(), body.toPrettyString());
        assertTrue(jdbcTemplate.queryForObject(
                "select observacoes from public.ferias_afastamentos where id = 1",
                String.class
        ).contains("[COBERTURA_PLANEJADA"));
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
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
