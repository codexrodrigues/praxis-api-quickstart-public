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
        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (2, 'Legacy Airspace Accord', 'Global Council', 'SUSPENSO', 'Suspended for audit')
                """);
        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (3, 'Retired Coastal Accord', 'Atlantic Board', 'REVOGADO', 'Revoked after treaty replacement')
                """);
        jdbcTemplate.update("""
                insert into public.equipes (id, nome, sigla, base_principal_id, status)
                values (1, 'Alpha Response Team', 'ALFA', null, 'ATIVA')
                """);
        jdbcTemplate.update("""
                insert into public.equipes (id, nome, sigla, base_principal_id, status)
                values (2, 'Legacy Response Team', 'LEG', null, 'DISSOLVIDA')
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
        JsonNode createSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/licencas-operacao&operation=post&schemaType=request",
                String.class
        ));
        JsonNode createTeamUi = createSchema.path("properties").path("equipeId").path("x-ui");
        assertAgreementLookup(
                createSchema.path("properties").path("acordoId").path("x-ui"),
                "entityLookup"
        );
        assertEquals("entityLookup", createTeamUi.path("controlType").asText());
        assertEquals("/api/operations/equipes/option-sources/team/options/filter",
                createTeamUi.path("endpoint").asText());
        assertEquals("team", createTeamUi.path("optionSource").path("key").asText());
        assertEquals("RESOURCE_ENTITY", createTeamUi.path("optionSource").path("type").asText());
        assertEquals("/api/operations/equipes", createTeamUi.path("optionSource").path("resourcePath").asText());
        assertEquals("team", createTeamUi.path("optionSource").path("entityKey").asText());
        assertEquals("status", createTeamUi.path("optionSource").path("statusPropertyPath").asText());
        assertEquals("ATIVA", createTeamUi.path("optionSource").path("selectionPolicy").path("allowedStatuses").get(0).asText());
        assertEquals("DISSOLVIDA", createTeamUi.path("optionSource").path("selectionPolicy").path("blockedStatuses").get(0).asText());
        assertEmployeeLookup(
                createSchema.path("properties").path("funcionarioId").path("x-ui"),
                "entityLookup"
        );

        JsonNode filterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/licencas-operacao/filter&operation=post&schemaType=request",
                String.class
        ));
        assertAgreementLookup(
                filterSchema.path("properties").path("acordoId").path("x-ui"),
                "inlineEntityLookup"
        );
        JsonNode filterTeamUi = filterSchema.path("properties").path("equipeId").path("x-ui");
        assertEquals("inlineEntityLookup", filterTeamUi.path("controlType").asText());
        assertEquals("team", filterTeamUi.path("optionSource").path("key").asText());
        assertEmployeeLookup(
                filterSchema.path("properties").path("funcionarioId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode memberSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/equipe-membros&operation=post&schemaType=request",
                String.class
        ));
        JsonNode memberTeamUi = memberSchema.path("properties").path("equipeId").path("x-ui");
        assertEquals("entityLookup", memberTeamUi.path("controlType").asText());
        assertEquals("team", memberTeamUi.path("optionSource").path("key").asText());
        assertEmployeeLookup(
                memberSchema.path("properties").path("funcionarioId").path("x-ui"),
                "entityLookup"
        );

        JsonNode memberFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/equipe-membros/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                memberFilterSchema.path("properties").path("funcionarioId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode payrollSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/human-resources/vw-analytics-folha-pagamento/filter&operation=post&schemaType=request",
                String.class
        ));
        JsonNode payrollTeamUi = payrollSchema.path("properties").path("equipeId").path("x-ui");
        assertEquals("inlineEntityLookup", payrollTeamUi.path("controlType").asText());
        assertEquals("team", payrollTeamUi.path("optionSource").path("key").asText());
        assertEquals("RESOURCE_ENTITY", payrollTeamUi.path("optionSource").path("type").asText());

        JsonNode teams = body(restTemplate.postForEntity(
                "/api/operations/equipes/option-sources/team/options/filter?search=Alpha",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, teams.path("content").size());
        JsonNode alpha = teams.path("content").get(0);
        assertEquals(1, alpha.path("id").asInt());
        assertEquals("Alpha Response Team", alpha.path("label").asText());
        assertTrue(alpha.path("extra").path("description").isMissingNode());
        assertTrue(alpha.path("extra").path("selectable").asBoolean());

        JsonNode selectedTeams = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/equipes/option-sources/team/options/by-ids?ids=1&ids=2",
                String.class
        ));
        assertEquals("Alpha Response Team", selectedTeams.get(0).path("label").asText());
        assertTrue(selectedTeams.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Legacy Response Team", selectedTeams.get(1).path("label").asText());
        assertFalse(selectedTeams.get(1).path("extra").path("selectable").asBoolean());

        JsonNode agreements = body(restTemplate.postForEntity(
                "/api/operations/acordos-regulatorios/option-sources/agreement/options/filter?search=Orbital",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, agreements.path("content").size());
        JsonNode orbitalAgreement = agreements.path("content").get(0);
        assertEquals(1, orbitalAgreement.path("id").asInt());
        assertEquals("Orbital Security Accord", orbitalAgreement.path("label").asText());
        assertTrue(orbitalAgreement.path("extra").path("selectable").asBoolean());

        JsonNode selectedAgreements = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/acordos-regulatorios/option-sources/agreement/options/by-ids?ids=1&ids=2&ids=3",
                String.class
        ));
        assertEquals("Orbital Security Accord", selectedAgreements.get(0).path("label").asText());
        assertTrue(selectedAgreements.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Legacy Airspace Accord", selectedAgreements.get(1).path("label").asText());
        assertFalse(selectedAgreements.get(1).path("extra").path("selectable").asBoolean());
        assertEquals("Retired Coastal Accord", selectedAgreements.get(2).path("label").asText());
        assertFalse(selectedAgreements.get(2).path("extra").path("selectable").asBoolean());

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

    private void assertEmployeeLookup(JsonNode fieldUi, String expectedControlType) {
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(expectedControlType, fieldUi.path("controlType").asText());
        assertEquals("/api/human-resources/funcionarios/option-sources/employee/options/filter",
                fieldUi.path("endpoint").asText());
        assertEquals("employee", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/human-resources/funcionarios", optionSource.path("resourcePath").asText());
        assertFalse(optionSource.hasNonNull("filterField"));
        assertEquals("employee", optionSource.path("entityKey").asText());
        assertEquals("nomeCompleto", optionSource.path("labelPropertyPath").asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
    }

    private void assertAgreementLookup(JsonNode fieldUi, String expectedControlType) {
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(expectedControlType, fieldUi.path("controlType").asText());
        assertEquals("/api/operations/acordos-regulatorios/option-sources/agreement/options/filter",
                fieldUi.path("endpoint").asText());
        assertEquals("agreement", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/operations/acordos-regulatorios", optionSource.path("resourcePath").asText());
        assertFalse(optionSource.hasNonNull("filterField"));
        assertEquals("agreement", optionSource.path("entityKey").asText());
        assertEquals("nome", optionSource.path("labelPropertyPath").asText());
        assertEquals("jurisdicao", optionSource.path("codePropertyPath").asText());
        assertEquals("status", optionSource.path("statusPropertyPath").asText());
        assertEquals("VIGENTE", optionSource.path("selectionPolicy").path("allowedStatuses").get(0).asText());
        assertEquals("SUSPENSO", optionSource.path("selectionPolicy").path("blockedStatuses").get(0).asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
    }
}
