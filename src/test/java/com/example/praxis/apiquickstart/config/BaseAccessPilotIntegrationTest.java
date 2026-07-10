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
                "spring.datasource.url=jdbc:h2:mem:quickstart_base_access_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_base_access_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class BaseAccessPilotIntegrationTest {

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
        jdbcTemplate.execute("drop table if exists public.base_acessos");
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("drop table if exists public.bases");

        jdbcTemplate.execute("""
                create table public.bases (
                    id integer primary key,
                    nome varchar(200) not null,
                    tipo varchar(40),
                    sigilo varchar(40),
                    latitude decimal(18,6),
                    longitude decimal(18,6),
                    planeta varchar(120)
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
                create table public.base_acessos (
                    id integer primary key,
                    base_id integer not null,
                    funcionario_id integer not null,
                    nivel_acesso varchar(255) not null,
                    ativo boolean not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.bases (id, nome, tipo, sigilo, latitude, longitude, planeta)
                values (1, 'Helicarrier Hub', 'MOVEL', 'SECRETA', 40.712800, -74.006000, 'Terra')
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (1, 'Maria Hill', '00000000001', DATE '1986-01-15', 'maria.hill@shield.org', '+55-11-99999-0001', 15000.00, DATE '2020-01-10', true, 1, 1, null, 'SOLTEIRO', 'Estados Unidos', 'Chicago')
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (2, 'Phil Coulson', '00000000002', DATE '1980-03-12', 'phil.coulson@shield.org', '+55-11-99999-0002', 14000.00, DATE '2018-05-02', true, 1, 1, null, 'SOLTEIRO', 'Estados Unidos', 'Portland')
                """);
        jdbcTemplate.update("""
                insert into public.base_acessos (id, base_id, funcionario_id, nivel_acesso, ativo)
                values (1, 1, 1, 'ALPHA', true)
                """);
        jdbcTemplate.update("""
                insert into public.base_acessos (id, base_id, funcionario_id, nivel_acesso, ativo)
                values (2, 1, 2, 'DELTA', false)
                """);
    }

    @Test
    void shouldExposeBaseResourceEntityLookupForAccessAndTeamSchemas() throws Exception {
        JsonNode accessSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/base-acessos&operation=post&schemaType=request",
                String.class
        ));
        JsonNode accessBaseUi = accessSchema.path("properties").path("baseId").path("x-ui");
        JsonNode accessOptionSource = accessBaseUi.path("optionSource");
        assertEquals("entityLookup", accessBaseUi.path("controlType").asText());
        assertEquals("/api/operations/bases/option-sources/base/options/filter",
                accessBaseUi.path("endpoint").asText());
        assertEquals("base", accessOptionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", accessOptionSource.path("type").asText());
        assertEquals("/api/operations/bases", accessOptionSource.path("resourcePath").asText());
        assertTrue(accessOptionSource.path("filterField").isMissingNode());
        assertEquals("base", accessOptionSource.path("entityKey").asText());
        assertEquals("tipo", accessOptionSource.path("descriptionPropertyPaths").get(0).asText());
        assertEquals("planeta", accessOptionSource.path("descriptionPropertyPaths").get(1).asText());
        assertEquals("nome", accessOptionSource.path("searchPropertyPaths").get(0).asText());
        assertTrue(accessOptionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(accessOptionSource.path("capabilities").path("byIds").asBoolean());
        assertEmployeeLookup(
                accessSchema.path("properties").path("funcionarioId").path("x-ui"),
                "entityLookup"
        );

        JsonNode accessFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/base-acessos/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                accessFilterSchema.path("properties").path("funcionarioId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode teamSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/equipes&operation=post&schemaType=request",
                String.class
        ));
        JsonNode teamBaseUi = teamSchema.path("properties").path("basePrincipalId").path("x-ui");
        assertEquals("entityLookup", teamBaseUi.path("controlType").asText());
        assertEquals("base", teamBaseUi.path("optionSource").path("key").asText());
        assertTrue(teamBaseUi.path("optionSource").path("filterField").isMissingNode());

        JsonNode payrollSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/human-resources/vw-analytics-folha-pagamento/filter&operation=post&schemaType=request",
                String.class
        ));
        JsonNode payrollBaseUi = payrollSchema.path("properties").path("baseId").path("x-ui");
        assertEquals("inlineEntityLookup", payrollBaseUi.path("controlType").asText());
        assertEquals("base", payrollBaseUi.path("optionSource").path("key").asText());
        assertEquals("RESOURCE_ENTITY", payrollBaseUi.path("optionSource").path("type").asText());
        assertTrue(payrollBaseUi.path("optionSource").path("filterField").isMissingNode());

        JsonNode bases = body(restTemplate.postForEntity(
                "/api/operations/bases/option-sources/base/options/filter?search=Helicarrier",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, bases.path("content").size());
        JsonNode helicarrier = bases.path("content").get(0);
        assertEquals(1, helicarrier.path("id").asInt());
        assertEquals("Helicarrier Hub", helicarrier.path("label").asText());
        assertEquals("MOVEL - Terra", helicarrier.path("extra").path("description").asText());
        assertTrue(helicarrier.path("extra").path("selectable").asBoolean());
        assertEquals("/api/operations/bases/1", helicarrier.path("extra").path("detailHref").asText());

        JsonNode selectedBases = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/bases/option-sources/base/options/by-ids?ids=1",
                String.class
        ));
        assertEquals("Helicarrier Hub", selectedBases.get(0).path("label").asText());
        assertTrue(selectedBases.get(0).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldExposeBaseSurfaceAndBaseAccessActions() throws Exception {
        JsonNode baseSurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.bases",
                String.class
        ));
        JsonNode opsContext = findById(baseSurfaces.path("surfaces"), "ops-context");
        assertNotNull(opsContext);
        assertEquals("/api/operations/bases/{id}/ops-context", opsContext.path("path").asText());

        JsonNode baseItemSurfaces = body(restTemplate.getForEntity(
                "/api/operations/bases/1/surfaces",
                String.class
        ));
        assertTrue(findById(baseItemSurfaces.path("surfaces"), "ops-context").path("availability").path("allowed").asBoolean());

        JsonNode accessSurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.base-acessos",
                String.class
        ));
        assertNotNull(findById(accessSurfaces.path("surfaces"), "review-access"));

        JsonNode accessActions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=operations.base-acessos",
                String.class
        ));
        assertNotNull(findById(accessActions.path("actions"), "activate"));
        assertNotNull(findById(accessActions.path("actions"), "deactivate"));

        JsonNode activeAccess = body(restTemplate.getForEntity(
                "/api/operations/base-acessos/1/actions",
                String.class
        ));
        assertTrue(findById(activeAccess.path("actions"), "deactivate").path("availability").path("allowed").asBoolean());
        assertFalse(findById(activeAccess.path("actions"), "activate").path("availability").path("allowed").asBoolean());

        JsonNode inactiveAccess = body(restTemplate.getForEntity(
                "/api/operations/base-acessos/2/capabilities",
                String.class
        ));
        assertTrue(findById(inactiveAccess.path("actions"), "activate").path("availability").path("allowed").asBoolean());
        assertNotNull(findById(inactiveAccess.path("surfaces"), "review-access"));

        JsonNode options = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/base-acessos/options/by-ids?ids=1",
                String.class
        ));
        assertTrue(options.get(0).path("label").asText().contains("Maria Hill"));
        assertTrue(options.get(0).path("label").asText().contains("Helicarrier Hub"));
    }

    @Test
    void shouldExecuteBaseContextAndBaseAccessTransitions() throws Exception {
        ResponseEntity<String> basePatch = restTemplate.exchange(
                "/api/operations/bases/1/ops-context",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "sigilo": "ULTRA_SECRETA",
                          "planeta": "Orbita Terrestre",
                          "latitude": 12.345678,
                          "longitude": -45.678912
                        }
                        """),
                String.class
        );
        JsonNode basePatchBody = body(basePatch);
        assertEquals("ULTRA_SECRETA", basePatchBody.path("data").path("sigilo").asText());
        assertEquals("Orbita Terrestre", basePatchBody.path("data").path("planeta").asText());

        ResponseEntity<String> reviewAccess = restTemplate.exchange(
                "/api/operations/base-acessos/1/review-access",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "nivelAcesso": "OMEGA"
                        }
                        """),
                String.class
        );
        JsonNode reviewBody = body(reviewAccess);
        assertEquals("OMEGA", reviewBody.path("data").path("nivelAcesso").asText());

        ResponseEntity<String> deactivate = restTemplate.exchange(
                "/api/operations/base-acessos/1/actions/deactivate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Incident review"
                        }
                        """),
                String.class
        );
        JsonNode deactivateBody = body(deactivate);
        assertEquals(false, deactivateBody.path("data").path("ativoAtual").asBoolean());
        assertFalse(deactivateBody.path("_links").path("schema").isMissingNode(), deactivateBody.toPrettyString());
        assertEquals(false, jdbcTemplate.queryForObject(
                "select ativo from public.base_acessos where id = 1",
                Boolean.class
        ));

        ResponseEntity<String> duplicateDeactivate = restTemplate.exchange(
                "/api/operations/base-acessos/1/actions/deactivate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Duplicate"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateDeactivate.getStatusCode());
        assertNotNull(duplicateDeactivate.getBody());
        JsonNode duplicateDeactivateBody = objectMapper.readTree(duplicateDeactivate.getBody());
        assertEquals("failure", duplicateDeactivateBody.path("status").asText());
        assertEquals("CONFLICT_DEPENDENCY", duplicateDeactivateBody.path("errors").get(0).path("outcome").asText());

        ResponseEntity<String> activate = restTemplate.exchange(
                "/api/operations/base-acessos/2/actions/activate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Mission cleared"
                        }
                        """),
                String.class
        );
        JsonNode activateBody = body(activate);
        assertTrue(activateBody.path("data").path("ativoAtual").asBoolean());
        assertFalse(activateBody.path("_links").path("schema").isMissingNode(), activateBody.toPrettyString());
        assertTrue(jdbcTemplate.queryForObject(
                "select ativo from public.base_acessos where id = 2",
                Boolean.class
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
}
