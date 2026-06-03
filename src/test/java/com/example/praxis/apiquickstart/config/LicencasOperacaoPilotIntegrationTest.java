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
                "spring.datasource.url=jdbc:h2:mem:quickstart_licencas_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_licencas_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class LicencasOperacaoPilotIntegrationTest {

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
    void seedTables() {
        LocalDate today = LocalDate.now();

        jdbcTemplate.execute("drop table if exists public.licencas_operacao");
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("drop table if exists public.equipes");
        jdbcTemplate.execute("drop table if exists public.acordos_regulatorios");

        jdbcTemplate.execute("""
                create table public.acordos_regulatorios (
                    id integer primary key,
                    nome varchar(200) not null,
                    jurisdicao varchar(200) not null,
                    status varchar(20) not null,
                    descricao varchar(4000)
                )
                """);

        jdbcTemplate.execute("""
                create table public.licencas_operacao (
                    id integer primary key,
                    acordo_id integer not null,
                    funcionario_id integer,
                    equipe_id integer,
                    nivel varchar(40),
                    valido_de date not null,
                    valido_ate date
                )
                """);

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
                create table public.equipes (
                    id integer primary key,
                    nome varchar(200) not null,
                    sigla varchar(12),
                    base_principal_id integer,
                    status varchar(40) not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (1, 'Orbital Security Accord', 'Global Council', 'VIGENTE', 'Supports orbital operations')
                """);

        jdbcTemplate.update(
                "insert into public.licencas_operacao (id, acordo_id, nivel, valido_de, valido_ate) values (?, ?, ?, ?, ?)",
                1, 1, "GLOBAL", java.sql.Date.valueOf(today.minusDays(5)), java.sql.Date.valueOf(today.plusDays(60))
        );
        jdbcTemplate.update(
                "insert into public.licencas_operacao (id, acordo_id, nivel, valido_de, valido_ate) values (?, ?, ?, ?, ?)",
                2, 1, "NACIONAL", java.sql.Date.valueOf(today.plusDays(10)), java.sql.Date.valueOf(today.plusDays(40))
        );
        jdbcTemplate.update(
                "insert into public.licencas_operacao (id, acordo_id, nivel, valido_de, valido_ate) values (?, ?, ?, ?, ?)",
                3, 1, "LOCAL", java.sql.Date.valueOf(today.minusDays(90)), java.sql.Date.valueOf(today.minusDays(2))
        );
        jdbcTemplate.update(
                "insert into public.licencas_operacao (id, acordo_id, nivel, valido_de, valido_ate) values (?, ?, ?, ?, ?)",
                4, 1, "EXTRATERRESTRE", java.sql.Date.valueOf(today.minusDays(20)), java.sql.Date.valueOf(today.plusDays(7))
        );
    }

    @Test
    void shouldExposeRenewSurfaceAndSemanticLookupForLicencasOperacao() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.licencas-operacao",
                String.class
        ));
        JsonNode renew = findById(surfacesCatalog.path("surfaces"), "renew");
        assertNotNull(renew);
        assertEquals("PATCH", renew.path("method").asText());
        assertEquals("ITEM", renew.path("scope").asText());
        assertEquals("/api/operations/licencas-operacao/{id}/renew", renew.path("path").asText());
        assertFalse(renew.path("availability").path("allowed").asBoolean());

        JsonNode activeSurfaces = body(restTemplate.getForEntity(
                "/api/operations/licencas-operacao/1/surfaces",
                String.class
        ));
        assertTrue(findById(activeSurfaces.path("surfaces"), "renew").path("availability").path("allowed").asBoolean());

        JsonNode futureCapabilities = body(restTemplate.getForEntity(
                "/api/operations/licencas-operacao/2/capabilities",
                String.class
        ));
        JsonNode futureRenew = findById(futureCapabilities.path("surfaces"), "renew");
        assertFalse(futureRenew.path("availability").path("allowed").asBoolean());
        assertEquals("resource-state-blocked", futureRenew.path("availability").path("reason").asText());

        JsonNode expiringCapabilities = body(restTemplate.getForEntity(
                "/api/operations/licencas-operacao/4/capabilities",
                String.class
        ));
        assertTrue(findById(expiringCapabilities.path("surfaces"), "renew").path("availability").path("allowed").asBoolean());

        JsonNode options = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/licencas-operacao/options/by-ids?ids=1",
                String.class
        ));
        assertTrue(options.get(0).path("label").asText().contains("Orbital Security Accord"));
        assertTrue(options.get(0).path("label").asText().contains("GLOBAL"));
    }

    @Test
    void shouldRenewExpiredLicenseAndRejectFutureLicenseRenewal() throws Exception {
        LocalDate today = LocalDate.now();

        ResponseEntity<String> renewResponse = restTemplate.exchange(
                "/api/operations/licencas-operacao/3/renew",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "nivel": "GLOBAL",
                          "validoDe": "%s",
                          "validoAte": "%s"
                        }
                        """.formatted(today, today.plusDays(120))),
                String.class
        );
        JsonNode renewBody = body(renewResponse);
        assertEquals("GLOBAL", renewBody.path("data").path("nivel").asText());
        assertEquals(today.toString(), jdbcTemplate.queryForObject(
                "select cast(valido_de as varchar) from public.licencas_operacao where id = 3",
                String.class
        ));

        ResponseEntity<String> conflictResponse = restTemplate.exchange(
                "/api/operations/licencas-operacao/2/renew",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "nivel": "GLOBAL",
                          "validoDe": "%s",
                          "validoAte": "%s"
                        }
                        """.formatted(today.plusDays(1), today.plusDays(90))),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
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
