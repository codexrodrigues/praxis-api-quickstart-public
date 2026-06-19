package com.example.praxis.apiquickstart.hr;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_employee_entity_lookup_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_employee_entity_lookup_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class FuncionarioEntityLookupIntegrationTest {

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
    void seedEmployeeTables() {
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("drop table if exists public.departamentos");
        jdbcTemplate.execute("drop table if exists public.cargos");

        jdbcTemplate.execute("""
                create table public.cargos (
                    id integer primary key,
                    nome varchar(120) not null,
                    nivel varchar(40) not null,
                    descricao varchar(255),
                    salario_minimo numeric(15, 2),
                    salario_maximo numeric(15, 2)
                )
                """);
        jdbcTemplate.execute("""
                create table public.departamentos (
                    id integer primary key,
                    nome varchar(120) not null,
                    codigo varchar(20) not null,
                    responsavel_id integer
                )
                """);
        jdbcTemplate.execute("""
                create table public.funcionarios (
                    id integer primary key,
                    nome_completo varchar(200) not null,
                    cpf varchar(11) not null,
                    data_nascimento date not null,
                    email varchar(200) not null,
                    telefone varchar(30) not null,
                    salario numeric(15, 2) not null,
                    data_admissao date not null,
                    ativo boolean not null,
                    cargo_id integer not null,
                    departamento_id integer not null,
                    foto_perfil_url varchar(255),
                    estado_civil varchar(40),
                    pais_nascimento varchar(120),
                    cidade_nascimento varchar(120)
                )
                """);

        jdbcTemplate.update("""
                insert into public.cargos
                    (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values
                    (10, 'Analista de Operacoes', 'PLENO', null, 5000.00, 9000.00),
                    (11, 'Coordenador de Campo', 'SENIOR', null, 9000.00, 14000.00)
                """);
        jdbcTemplate.update("""
                insert into public.departamentos
                    (id, nome, codigo, responsavel_id)
                values
                    (20, 'Operacoes', 'OPS', null),
                    (21, 'Inteligencia', 'INT', null)
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios
                    (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo,
                     cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values
                    (1, 'Diana Prince', '11111111111', DATE '1990-05-01', 'diana@example.com', '+5511999999991', 12500.00, DATE '2024-01-15', true, 11, 20, null, 'SOLTEIRO', 'Brasil', 'Sao Paulo'),
                    (2, 'Barbara Gordon', '22222222222', DATE '1992-08-10', 'barbara@example.com', '+5511999999992', 8400.00, DATE '2024-04-03', true, 10, 21, null, 'SOLTEIRO', 'Brasil', 'Rio de Janeiro'),
                    (3, 'Bruce Wayne', '33333333333', DATE '1985-02-19', 'bruce@example.com', '+5511999999993', 15000.00, DATE '2023-10-20', false, 11, 20, null, 'CASADO', 'Brasil', 'Curitiba')
                """);
    }

    @Test
    void shouldExposeEmployeeEntityLookupMetadataAndExecuteRealSearch() throws Exception {
        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.Operations.MISSAO_PARTICIPANTES
        ));

        JsonNode funcionarioField = schema.path("properties").path("funcionarioId").path("x-ui");
        JsonNode optionSource = funcionarioField.path("optionSource");
        assertEquals("entityLookup", funcionarioField.path("controlType").asText());
        assertEquals("employee", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS, optionSource.path("resourcePath").asText());
        assertEquals("nomeCompleto", optionSource.path("labelPropertyPath").asText());
        assertEquals("ativo", optionSource.path("selectionPolicy").path("selectablePropertyPath").asText());
        assertEquals("surface", optionSource.path("detail").path("kind").asText());
        assertEquals("view", optionSource.path("detail").path("surfaceId").asText());
        assertEquals("drawer", optionSource.path("detail").path("presentation").asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
        assertFalse(optionSource.path("capabilities").path("create").asBoolean());

        JsonNode employees = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=Diana&page=0&size=5",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, employees.path("content").size());
        JsonNode diana = employees.path("content").get(0);
        assertEquals(1, diana.path("id").asInt());
        assertEquals("Diana Prince", diana.path("label").asText());
        assertTrue(diana.path("extra").path("code").isMissingNode());
        assertEquals("Coordenador de Campo - Operacoes", diana.path("extra").path("description").asText());
        assertTrue(diana.path("extra").path("selectable").asBoolean());
        assertTrue(diana.path("extra").path("detailHref").isMissingNode());
        assertEquals("employee", diana.path("extra").path("entityKey").asText());
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS, diana.path("extra").path("resourcePath").asText());

        JsonNode byIds = objectMapper.readTree(restTemplate.getForObject(
                "/api/human-resources/funcionarios/option-sources/employee/options/by-ids?ids=3",
                String.class
        ));
        assertEquals("Bruce Wayne", byIds.get(0).path("label").asText());
        assertFalse(byIds.get(0).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldReuseEmployeeEntityLookupAcrossHumanResourcesRelationships() throws Exception {
        assertEmployeeLookup(ApiPaths.HumanResources.DEPARTAMENTOS, "responsavelId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.DEPENDENTES, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.ENDERECOS, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FERIAS_AFASTAMENTOS, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FOLHAS_PAGAMENTO, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.HISTORICOS_SALARIAIS, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.HISTORICOS_CARGOS, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.IDENTIDADES_SECRETAS, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.MENCOES_MIDIA, "funcionarioId", "entityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.REPUTACOES, "funcionarioId", "entityLookup");

        assertEmployeeLookup(ApiPaths.HumanResources.DEPARTAMENTOS + "/filter", "responsavelId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.DEPENDENTES + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.ENDERECOS + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FERIAS_AFASTAMENTOS + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FOLHAS_PAGAMENTO + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.HISTORICOS_SALARIAIS + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.HISTORICOS_CARGOS + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.IDENTIDADES_SECRETAS + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.MENCOES_MIDIA + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.REPUTACOES + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/filter", "funcionarioIdsIn", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.VW_PERFIL_HEROI + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.VW_PERFIL_HEROI + "/filter", "funcionarioIdsIn", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.VW_RANKING_REPUTACAO + "/filter", "funcionarioId", "inlineEntityLookup");
        assertEmployeeLookup(ApiPaths.HumanResources.VW_RANKING_REPUTACAO + "/filter", "funcionarioIdsIn", "inlineEntityLookup");
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private void assertEmployeeLookup(String path, String fieldName, String expectedControlType) throws Exception {
        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                path
        ));

        JsonNode fieldUi = schema.path("properties").path(fieldName).path("x-ui");
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(expectedControlType, fieldUi.path("controlType").asText(), path + "#" + fieldName);
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, fieldUi.path("endpoint").asText(), path + "#" + fieldName);
        assertEquals("employee", optionSource.path("key").asText(), path + "#" + fieldName);
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText(), path + "#" + fieldName);
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS, optionSource.path("resourcePath").asText(), path + "#" + fieldName);
        assertEquals("employee", optionSource.path("entityKey").asText(), path + "#" + fieldName);
        assertEquals("id", optionSource.path("valuePropertyPath").asText(), path + "#" + fieldName);
        assertEquals("nomeCompleto", optionSource.path("labelPropertyPath").asText(), path + "#" + fieldName);
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean(), path + "#" + fieldName);
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean(), path + "#" + fieldName);
    }

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }
}
