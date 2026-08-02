package com.example.praxis.apiquickstart.hr;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.hr.options.EmployeeOptionSourceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.hr.analytics.demo-department-scopes=demo-manager=20",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.resource-version.etag.secret=test-secret-resource-version",
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
@ExtendWith(OutputCaptureExtension.class)
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

    @SpyBean
    private EmployeeOptionSourceProvider employeeOptionSourceProvider;

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
                    version bigint not null default 0,
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
                    (3, 'Bruce Wayne', '33333333333', DATE '1985-02-19', 'bruce@example.com', '+5511999999993', 15000.00, DATE '2023-10-20', false, 11, 20, null, 'CASADO', 'Brasil', 'Curitiba'),
                    (4, 'Maria Costa', '44444444444', DATE '1991-03-14', 'maria.costa@example.com', '+5511999999994', 9100.00, DATE '2024-02-01', true, 10, 20, null, 'SOLTEIRO', 'Brasil', 'Recife'),
                    (5, 'Maria Silva', '55555555555', DATE '1993-06-18', 'maria.silva@example.com', '+5511999999995', 9200.00, DATE '2024-02-02', true, 10, 21, null, 'SOLTEIRO', 'Brasil', 'Salvador')
                """);
    }

    @Test
    void shouldExposeEmployeeEntityLookupMetadataAndExecuteGovernedStrategies(CapturedOutput output) throws Exception {
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
        assertEquals("required", optionSource.path("selectedReloadPolicy").asText());
        assertEquals("reject", optionSource.path("invalidSortPolicy").asText());
        JsonNode strategies = optionSource.path("filtering").path("searchStrategies");
        assertEquals(3, strategies.size());
        assertEquals("employee-code", strategies.get(0).path("key").asText());
        assertEquals("digits", strategies.get(0).path("inputFormat").asText());
        assertEquals("name", strategies.get(1).path("key").asText());
        assertEquals("document", strategies.get(2).path("key").asText());

        JsonNode employees = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=Diana&searchStrategy=name&page=0&size=5",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, employees.path("content").size());
        JsonNode diana = employees.path("content").get(0);
        assertEquals(1, diana.path("id").asInt());
        assertEquals("Diana Prince · CPF ***.***.***-11", diana.path("label").asText());
        assertTrue(diana.path("extra").path("code").isMissingNode());
        assertTrue(diana.path("extra").path("description").isMissingNode());
        assertTrue(diana.path("extra").path("selectable").asBoolean());
        assertTrue(diana.path("extra").path("detailHref").isMissingNode());
        assertEquals("employee", diana.path("extra").path("entityKey").asText());
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS, diana.path("extra").path("resourcePath").asText());

        JsonNode byCode = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=1&searchStrategy=employee-code&page=0&size=5",
                authorizedJson("{}"), String.class));
        assertEquals(1, byCode.path("content").size());
        assertEquals(1, byCode.path("content").get(0).path("id").asInt());

        JsonNode byFormattedDocument = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=111.111.111-11&searchStrategy=document&page=0&size=5",
                authorizedJson("{}"), String.class));
        JsonNode byPlainDocument = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=11111111111&searchStrategy=document&page=0&size=5",
                authorizedJson("{}"), String.class));
        assertEquals(byFormattedDocument.path("content"), byPlainDocument.path("content"));
        assertFalse(byFormattedDocument.toString().contains("11111111111"));

        JsonNode orderedNames = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?page=0&size=5",
                authorizedJson("{\"search\":\"Maria\",\"searchStrategy\":\"name\",\"sort\":\"labelAsc\"}"),
                String.class));
        assertEquals(List.of("Maria Costa · CPF ***.***.***-44", "Maria Silva · CPF ***.***.***-55"),
                java.util.stream.StreamSupport.stream(orderedNames.path("content").spliterator(), false)
                        .map(item -> item.path("label").asText()).toList());

        JsonNode byIds = body(restTemplate.exchange(
                "/api/human-resources/funcionarios/option-sources/employee/options/by-ids?ids=3&ids=1",
                HttpMethod.GET, new HttpEntity<>(authorizedJson("").getHeaders()), String.class));
        assertEquals("Bruce Wayne · CPF ***.***.***-33", byIds.get(0).path("label").asText());
        assertEquals("Diana Prince · CPF ***.***.***-11", byIds.get(1).path("label").asText());
        assertFalse(byIds.get(0).path("extra").path("selectable").asBoolean());

        String openApi = restTemplate.getForObject(
                "/v3/api-docs/api-human-resources-funcionarios", String.class);
        assertNotNull(openApi);
        assertFalse(openApi.contains("EmployeeOptionSourceProvider"));
        assertFalse(openApi.contains("authenticatedSubject"));
        assertFalse(openApi.contains("departmentScopeIds"));
        assertFalse(openApi.contains("11111111111"));
        assertFalse(output.getAll().contains("11111111111"));
    }

    @Test
    void shouldRejectAmbiguousInvalidAndUnauthorizedSearchesBeforeDataLeaks() throws Exception {
        reset(employeeOptionSourceProvider);
        ResponseEntity<String> invalidCode = restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=EMP-1&searchStrategy=employee-code&page=0&size=5",
                authorizedJson("{}"), String.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, invalidCode.getStatusCode());
        verify(employeeOptionSourceProvider, never()).supports(any(), any(), any());
        verify(employeeOptionSourceProvider, never()).filter(any());

        ResponseEntity<String> ambiguous = restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=Ana&page=0&size=5",
                authorizedJson("{}"), String.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ambiguous.getStatusCode());

        ResponseEntity<String> anonymous = restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?search=Ana&searchStrategy=name&page=0&size=5",
                new HttpEntity<>("{}", jsonHeaders()), String.class);
        assertEquals(HttpStatus.FORBIDDEN, anonymous.getStatusCode());
        assertFalse(String.valueOf(anonymous.getBody()).contains("departmentScope"));

        ResponseEntity<String> anonymousEmptyReload = restTemplate.exchange(
                "/api/human-resources/funcionarios/option-sources/employee/options/by-ids",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, anonymousEmptyReload.getStatusCode());
        assertFalse(String.valueOf(anonymousEmptyReload.getBody()).contains("departmentScope"));

        JsonNode scoped = body(restTemplate.postForEntity(
                "/api/human-resources/funcionarios/option-sources/employee/options/filter?searchStrategy=name&page=0&size=5",
                authorizedJson("{\"search\":\"Maria\"}", "demo-manager", "USER"), String.class));
        assertEquals(1, scoped.path("content").size());
        assertEquals(4, scoped.path("content").get(0).path("id").asInt());
        assertFalse(scoped.toString().contains("Maria Silva"));
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

    @Test
    void shouldExposeEmployeeRelationshipSurfacesForCockpitNavigation() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.funcionarios",
                String.class
        ));

        JsonNode heroProfile = findById(surfacesCatalog.path("surfaces"), "hero-profile");
        assertNotNull(heroProfile);
        assertEquals("READ_PROJECTION", heroProfile.path("kind").asText());
        assertEquals("human-resources.vw-perfil-heroi", heroProfile.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.VW_PERFIL_HEROI, heroProfile.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", heroProfile.path("relatedResource").path("childParentField").asText());
        assertTrue(heroProfile.path("relatedResource").path("selectable").asBoolean());
        assertEquals("funcionarioId", heroProfile.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[]", heroProfile.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(heroProfile);

        JsonNode payrollHistory = findById(surfacesCatalog.path("surfaces"), "payroll-history");
        assertNotNull(payrollHistory);
        assertEquals("READ_PROJECTION", payrollHistory.path("kind").asText());
        assertEquals("human-resources.vw-analytics-folha-pagamento",
                payrollHistory.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                payrollHistory.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", payrollHistory.path("relatedResource").path("childParentField").asText());
        assertTrue(payrollHistory.path("relatedResource").path("selectable").asBoolean());
        assertEquals("folhaPagamentoId", payrollHistory.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\"]",
                payrollHistory.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(payrollHistory);

        JsonNode missionParticipations = findById(surfacesCatalog.path("surfaces"), "mission-participations");
        assertNotNull(missionParticipations);
        assertEquals("READ_PROJECTION", missionParticipations.path("kind").asText());
        assertEquals("operations.missao-participantes",
                missionParticipations.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.Operations.MISSAO_PARTICIPANTES,
                missionParticipations.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", missionParticipations.path("relatedResource").path("childParentField").asText());
        assertTrue(missionParticipations.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", missionParticipations.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                missionParticipations.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(missionParticipations);

        JsonNode dependents = findById(surfacesCatalog.path("surfaces"), "dependents");
        assertNotNull(dependents);
        assertEquals("READ_PROJECTION", dependents.path("kind").asText());
        assertEquals("human-resources.dependentes",
                dependents.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.DEPENDENTES,
                dependents.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", dependents.path("relatedResource").path("childParentField").asText());
        assertTrue(dependents.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", dependents.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                dependents.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(dependents);

        JsonNode address = findById(surfacesCatalog.path("surfaces"), "address");
        assertNotNull(address);
        assertEquals("READ_PROJECTION", address.path("kind").asText());
        assertEquals("human-resources.enderecos",
                address.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.ENDERECOS,
                address.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", address.path("relatedResource").path("childParentField").asText());
        assertTrue(address.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", address.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                address.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(address);

        JsonNode skills = findById(surfacesCatalog.path("surfaces"), "skills");
        assertNotNull(skills);
        assertEquals("READ_PROJECTION", skills.path("kind").asText());
        assertEquals("human-resources.funcionario-habilidades",
                skills.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES,
                skills.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", skills.path("relatedResource").path("childParentField").asText());
        assertTrue(skills.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", skills.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                skills.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(skills);

        JsonNode careerHistory = findById(surfacesCatalog.path("surfaces"), "career-history");
        assertNotNull(careerHistory);
        assertEquals("READ_PROJECTION", careerHistory.path("kind").asText());
        assertEquals("human-resources.historicos-cargos",
                careerHistory.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.HumanResources.HISTORICOS_CARGOS,
                careerHistory.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", careerHistory.path("relatedResource").path("childParentField").asText());
        assertTrue(careerHistory.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", careerHistory.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                careerHistory.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(careerHistory);

        JsonNode equipmentCustody = findById(surfacesCatalog.path("surfaces"), "equipment-custody");
        assertNotNull(equipmentCustody);
        assertEquals("READ_PROJECTION", equipmentCustody.path("kind").asText());
        assertEquals("assets.equipamento-alocacoes",
                equipmentCustody.path("relatedResource").path("childResourceKey").asText());
        assertEquals(ApiPaths.Assets.EQUIPAMENTO_ALOCACOES,
                equipmentCustody.path("relatedResource").path("childResourcePath").asText());
        assertEquals("funcionarioId", equipmentCustody.path("relatedResource").path("childParentField").asText());
        assertTrue(equipmentCustody.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", equipmentCustody.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                equipmentCustody.path("relatedResource").path("childOperations").toString());
        assertRelatedResourceFieldsExistInResponseSchema(equipmentCustody);
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode findById(JsonNode nodes, String id) {
        for (JsonNode node : nodes) {
            if (id.equals(node.path("id").asText())) {
                return node;
            }
        }
        return null;
    }

    private void assertRelatedResourceFieldsExistInResponseSchema(JsonNode surface) throws Exception {
        JsonNode relatedResource = surface.path("relatedResource");
        JsonNode properties = body(restTemplate.getForEntity(surface.path("schemaUrl").asText(), String.class))
                .path("properties");

        String childParentField = relatedResource.path("childParentField").asText();
        assertTrue(properties.has(childParentField), surface.path("id").asText() + " childParentField");

        if (relatedResource.path("selectable").asBoolean()) {
            String selectionKeyField = relatedResource.path("selectionKeyField").asText();
            assertTrue(properties.has(selectionKeyField), surface.path("id").asText() + " selectionKeyField");
        }
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
        return authorizedJson(json, "admin", "ADMIN");
    }

    private HttpEntity<String> authorizedJson(String json, String subject, String role) {
        HttpHeaders headers = jsonHeaders();
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(subject, role));
        return new HttpEntity<>(json, headers);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
