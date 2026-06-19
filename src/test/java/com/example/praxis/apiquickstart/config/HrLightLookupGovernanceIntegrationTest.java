package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_hr_light_lookup_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_hr_light_lookup_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class HrLightLookupGovernanceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private OptionSourceRegistry optionSourceRegistry;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @MockBean
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @BeforeEach
    void seedTables() {
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.cargos");
        jdbcTemplate.execute("drop table if exists public.departamentos");
        jdbcTemplate.execute("drop table if exists public.habilidades");

        jdbcTemplate.execute("""
                create table public.cargos (
                    id integer primary key,
                    nome varchar(200) not null,
                    nivel varchar(80) not null,
                    descricao varchar(2000),
                    salario_minimo numeric(19, 2),
                    salario_maximo numeric(19, 2)
                )
                """);
        jdbcTemplate.execute("""
                create table public.departamentos (
                    id integer primary key,
                    nome varchar(200) not null,
                    codigo varchar(20) not null,
                    responsavel_id integer
                )
                """);
        jdbcTemplate.execute("""
                create table public.habilidades (
                    id integer primary key,
                    nome varchar(200) not null,
                    categoria varchar(40),
                    descricao varchar(2000),
                    nivel_poder integer
                )
                """);

        jdbcTemplate.update("""
                insert into public.cargos (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values (1, 'Analista de Campo', 'SENIOR', 'Atuacao operacional em campo', 12000, 18000)
                """);
        jdbcTemplate.update("""
                insert into public.cargos (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values (2, 'Coordenador Tatico', 'LEAD', 'Coordenacao de equipe', 18000, 24000)
                """);
        jdbcTemplate.update("""
                insert into public.departamentos (id, nome, codigo, responsavel_id)
                values (10, 'Operacoes Globais', 'OPS', null)
                """);
        jdbcTemplate.update("""
                insert into public.departamentos (id, nome, codigo, responsavel_id)
                values (11, 'Pesquisa Aplicada', 'PESQ', null)
                """);
        jdbcTemplate.update("""
                insert into public.habilidades (id, nome, categoria, descricao, nivel_poder)
                values (20, 'Telepatia', 'MENTAL', 'Comunicacao mental e leitura situacional', 8)
                """);
        jdbcTemplate.update("""
                insert into public.habilidades (id, nome, categoria, descricao, nivel_poder)
                values (21, 'Engenharia Arcana', 'MISTICA', 'Construcao de artefatos misticos', 7)
                """);
    }

    @Test
    void shouldRegisterHrCatalogsAsLightLookupNotResourceEntity() {
        Map<String, OptionSourceDescriptor> descriptors = optionSourceRegistry.descriptors()
                .stream()
                .filter(descriptor -> descriptor.type() == OptionSourceType.LIGHT_LOOKUP)
                .collect(Collectors.toMap(OptionSourceDescriptor::key, descriptor -> descriptor));

        assertLightLookupDescriptor(
                descriptors.get(ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE),
                ApiPaths.HumanResources.CARGOS
        );
        assertLightLookupDescriptor(
                descriptors.get(ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE),
                ApiPaths.HumanResources.DEPARTAMENTOS
        );
        assertLightLookupDescriptor(
                descriptors.get(ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_SOURCE),
                ApiPaths.HumanResources.HABILIDADES
        );
    }

    @Test
    void shouldPublishHrLightLookupsInFilteredSchemas() throws Exception {
        JsonNode employeeSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.FUNCIONARIOS
        ));
        assertLightLookupUi(
                employeeSchema.path("properties").path("cargoId").path("x-ui"),
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.CARGOS,
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS
        );
        assertLightLookupUi(
                employeeSchema.path("properties").path("departamentoId").path("x-ui"),
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE,
                ApiPaths.HumanResources.DEPARTAMENTOS,
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS
        );

        JsonNode skillAssignmentSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES
        ));
        assertLightLookupUi(
                skillAssignmentSchema.path("properties").path("habilidadeId").path("x-ui"),
                ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_SOURCE,
                ApiPaths.HumanResources.HABILIDADES,
                ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_OPTIONS
        );

        JsonNode employeeFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.FUNCIONARIOS + "/filter"
        ));
        assertLightLookupUi(
                employeeFilterSchema.path("properties").path("cargoIdsIn").path("x-ui"),
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.CARGOS,
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS
        );
        assertLightLookupUi(
                employeeFilterSchema.path("properties").path("departamentoIdsIn").path("x-ui"),
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE,
                ApiPaths.HumanResources.DEPARTAMENTOS,
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS
        );

        JsonNode careerFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.HISTORICOS_CARGOS + "/filter"
        ));
        assertLightLookupUi(
                careerFilterSchema.path("properties").path("cargoId").path("x-ui"),
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.CARGOS,
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS
        );

        JsonNode skillAssignmentFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES + "/filter"
        ));
        assertLightLookupUi(
                skillAssignmentFilterSchema.path("properties").path("habilidadeId").path("x-ui"),
                ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_SOURCE,
                ApiPaths.HumanResources.HABILIDADES,
                ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_OPTIONS
        );

        JsonNode payrollAnalyticsFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO + "/filter"
        ));
        assertLightLookupUi(
                payrollAnalyticsFilterSchema.path("properties").path("cargoId").path("x-ui"),
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.CARGOS,
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS
        );
        assertLightLookupUi(
                payrollAnalyticsFilterSchema.path("properties").path("departamentoId").path("x-ui"),
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE,
                ApiPaths.HumanResources.DEPARTAMENTOS,
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS
        );
    }

    @Test
    void shouldExecuteHrLightLookupEndpoints() throws Exception {
        JsonNode cargos = body(restTemplate.postForEntity(
                ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS + "?search=Analista&page=0&size=5",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, cargos.path("content").size());
        assertEquals(1, cargos.path("content").get(0).path("id").asInt());
        assertEquals("Analista de Campo", cargos.path("content").get(0).path("label").asText());
        assertTrue(cargos.path("content").get(0).path("extra").isNull());

        JsonNode selectedCargos = objectMapper.readTree(restTemplate.getForObject(
                ApiPaths.HumanResources.CARGOS + "/option-sources/"
                        + ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE
                        + "/options/by-ids?ids=2&ids=1",
                String.class
        ));
        assertEquals(2, selectedCargos.size());
        assertEquals("Coordenador Tatico", selectedCargos.get(0).path("label").asText());
        assertEquals("Analista de Campo", selectedCargos.get(1).path("label").asText());

        JsonNode departamentos = body(restTemplate.postForEntity(
                ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS + "?search=Pesquisa",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, departamentos.path("content").size());
        assertEquals("Pesquisa Aplicada", departamentos.path("content").get(0).path("label").asText());
        assertTrue(departamentos.path("content").get(0).path("extra").isNull());

        JsonNode selectedDepartamentos = objectMapper.readTree(restTemplate.getForObject(
                ApiPaths.HumanResources.DEPARTAMENTOS + "/option-sources/"
                        + ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE
                        + "/options/by-ids?ids=11&ids=10",
                String.class
        ));
        assertEquals(2, selectedDepartamentos.size());
        assertEquals("Pesquisa Aplicada", selectedDepartamentos.get(0).path("label").asText());
        assertEquals("Operacoes Globais", selectedDepartamentos.get(1).path("label").asText());

        JsonNode habilidades = body(restTemplate.postForEntity(
                ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_OPTIONS + "?search=Telepatia",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, habilidades.path("content").size());
        assertEquals("Telepatia", habilidades.path("content").get(0).path("label").asText());
        assertTrue(habilidades.path("content").get(0).path("extra").isNull());

        JsonNode selectedHabilidades = objectMapper.readTree(restTemplate.getForObject(
                ApiPaths.HumanResources.HABILIDADES + "/option-sources/"
                        + ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_SOURCE
                        + "/options/by-ids?ids=21&ids=20",
                String.class
        ));
        assertEquals(2, selectedHabilidades.size());
        assertEquals("Engenharia Arcana", selectedHabilidades.get(0).path("label").asText());
        assertEquals("Telepatia", selectedHabilidades.get(1).path("label").asText());
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

    private void assertLightLookupDescriptor(OptionSourceDescriptor descriptor, String resourcePath) {
        assertNotNull(descriptor);
        assertEquals(OptionSourceType.LIGHT_LOOKUP, descriptor.type());
        assertEquals(resourcePath, descriptor.resourcePath());
        assertEquals("id", descriptor.valuePropertyPath());
        assertEquals("nome", descriptor.labelPropertyPath());
        assertNull(descriptor.entityLookup());
    }

    private void assertLightLookupUi(JsonNode fieldUi, String sourceKey, String resourcePath, String endpoint) {
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(endpoint, fieldUi.path("endpoint").asText());
        assertEquals(sourceKey, optionSource.path("key").asText());
        assertEquals("LIGHT_LOOKUP", optionSource.path("type").asText());
        assertEquals(resourcePath, optionSource.path("resourcePath").asText());
        assertEquals("id", optionSource.path("valuePropertyPath").asText());
        assertEquals("nome", optionSource.path("labelPropertyPath").asText());
        assertFalse(optionSource.hasNonNull("entityKey"));
        assertFalse(optionSource.hasNonNull("capabilities"));
    }
}
