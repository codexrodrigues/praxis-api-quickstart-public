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
                "spring.datasource.url=jdbc:h2:mem:quickstart_missoes_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_missoes_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class MissaoPilotIntegrationTest {

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

        jdbcTemplate.execute("drop table if exists public.vw_resumo_missoes");
        jdbcTemplate.execute("drop table if exists public.missao_eventos");
        jdbcTemplate.execute("drop table if exists public.missao_participantes");
        jdbcTemplate.execute("drop table if exists public.equipe_membros");
        jdbcTemplate.execute("drop table if exists public.equipes");
        jdbcTemplate.execute("drop table if exists public.bases");
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("drop table if exists public.missoes");
        jdbcTemplate.execute("drop table if exists public.ameacas");
        jdbcTemplate.execute("drop table if exists public.cargos");
        jdbcTemplate.execute("drop table if exists public.departamentos");

        jdbcTemplate.execute("""
                create table public.cargos (
                    id integer primary key,
                    nome varchar(200) not null,
                    nivel varchar(100) not null,
                    descricao varchar(500),
                    salario_minimo decimal(18,2),
                    salario_maximo decimal(18,2)
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
                create table public.ameacas (
                    id integer primary key,
                    nome varchar(200) not null,
                    classe varchar(40),
                    planeta varchar(100),
                    nivel integer,
                    status varchar(40) not null,
                    recompensa decimal(18,2)
                )
                """);

        jdbcTemplate.execute("""
                create table public.bases (
                    id integer primary key,
                    nome varchar(200) not null,
                    tipo varchar(40),
                    sigilo varchar(40),
                    latitude decimal(12,8),
                    longitude decimal(12,8),
                    planeta varchar(100)
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

        jdbcTemplate.execute("""
                create table public.equipe_membros (
                    id integer primary key,
                    equipe_id integer not null,
                    funcionario_id integer not null,
                    papel varchar(40),
                    data_entrada date not null,
                    data_saida date
                )
                """);

        jdbcTemplate.execute("""
                create table public.missao_participantes (
                    id integer primary key,
                    missao_id integer not null,
                    funcionario_id integer not null,
                    papel varchar(30),
                    ordem integer not null default 0,
                    principal boolean not null default false,
                    resultado varchar(30)
                )
                """);

        jdbcTemplate.execute("""
                create table public.missao_eventos (
                    id integer primary key,
                    missao_id integer not null,
                    ocorrido_em timestamp with time zone not null,
                    tipo varchar(30),
                    descricao varchar(4000)
                )
                """);

        jdbcTemplate.execute("""
                create table public.vw_resumo_missoes (
                    missao_id integer primary key,
                    titulo varchar(200),
                    status varchar(30),
                    prioridade varchar(30),
                    local varchar(200),
                    ameaca varchar(200),
                    qtd_herois bigint,
                    qtd_eventos bigint,
                    primeira_acao timestamp with time zone,
                    ultima_acao timestamp with time zone
                )
                """);

        jdbcTemplate.update("""
                insert into public.cargos (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values (1, 'Agente de Campo', 'Senior', 'Resposta operacional', 9000.00, 19000.00)
                """);
        jdbcTemplate.update("""
                insert into public.departamentos (id, nome, codigo, responsavel_id)
                values (1, 'Operações', 'OPS', null)
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (1, 'Diana Prince', '00000000001', DATE '1984-03-22', 'diana@justice.org', '+55-11-97000-0001', 21000.00, DATE '2020-01-10', true, 1, 1, null, 'SOLTEIRO', 'Themyscira', 'Themyscira')
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios (id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento)
                values (2, 'Victor Stone', '00000000002', DATE '1994-08-18', 'victor@justice.org', '+55-11-97000-0002', 18500.00, DATE '2021-05-02', true, 1, 1, null, 'SOLTEIRO', 'Estados Unidos', 'Detroit')
                """);
        jdbcTemplate.update("""
                insert into public.bases (id, nome, tipo, sigilo, latitude, longitude, planeta)
                values (1, 'Base Atlântica', 'TERRESTRE', 'CONFIDENCIAL', -23.55052000, -46.63330800, 'Terra')
                """);
        jdbcTemplate.update("""
                insert into public.equipes (id, nome, sigla, base_principal_id, status)
                values (1, 'Equipe Atlântica', 'ATL-1', 1, 'ATIVA')
                """);
        jdbcTemplate.update("""
                insert into public.equipe_membros (id, equipe_id, funcionario_id, papel, data_entrada, data_saida)
                values (1, 1, 1, 'LIDER', DATE '2025-01-10', null)
                """);
        jdbcTemplate.update("""
                insert into public.equipe_membros (id, equipe_id, funcionario_id, papel, data_entrada, data_saida)
                values (2, 1, 2, 'SUPORTE', DATE '2025-02-15', null)
                """);

        jdbcTemplate.execute("""
                create table public.missoes (
                    id integer primary key,
                    titulo varchar(200) not null,
                    objetivo varchar(4000),
                    prioridade varchar(20) not null,
                    status varchar(20) not null,
                    local varchar(200),
                    ameaca_id integer,
                    inicio_prev timestamp with time zone,
                    fim_prev timestamp with time zone,
                    inicio_real timestamp with time zone,
                    fim_real timestamp with time zone
                )
                """);

        jdbcTemplate.update("""
                insert into public.ameacas (id, nome, classe, planeta, nivel, status, recompensa)
                values (1, 'Omega Swarm', 'INVASAO', 'Terra', 9, 'EM_OBSERVACAO', 500000.00)
                """);
        jdbcTemplate.update("""
                insert into public.ameacas (id, nome, classe, planeta, nivel, status, recompensa)
                values (2, 'Captured Syndicate', 'ORGANIZACAO', 'Marte', 6, 'CAPTURADO', 125000.00)
                """);

        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (1, 'Atlantic Shield', 'Protect the coastal corridor', 'CRITICA', 'PLANEJADA', 'Atlantis', 1,
                        TIMESTAMP WITH TIME ZONE '2026-04-02 09:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 18:00:00+00:00',
                        null, null)
                """);
        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (2, 'Sky Bridge', 'Secure the orbital bridge', 'ALTA', 'EM_ANDAMENTO', 'Orbital Gate', 1,
                        TIMESTAMP WITH TIME ZONE '2026-04-02 10:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 22:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 10:15:00+00:00', null)
                """);
        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (3, 'Silent Reef', 'Hold the reef perimeter', 'MEDIA', 'PAUSADA', 'Pacific Reef', 1,
                        TIMESTAMP WITH TIME ZONE '2026-04-03 08:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-03 18:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-03 08:30:00+00:00', null)
                """);
        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (4, 'Frozen Wall', 'Close the arctic breach', 'ALTA', 'CONCLUIDA', 'Arctic Gate', 1,
                        TIMESTAMP WITH TIME ZONE '2026-04-01 07:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-01 16:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-01 07:15:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-01 15:40:00+00:00')
                """);
        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (5, 'Ashen Trail', 'Evacuate the ash corridor', 'MEDIA', 'FALHOU', 'Volcanic Rim', 1,
                        TIMESTAMP WITH TIME ZONE '2026-03-28 09:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-03-28 19:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-03-28 09:20:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-03-28 11:10:00+00:00')
                """);

        jdbcTemplate.update("""
                insert into public.missao_participantes (id, missao_id, funcionario_id, papel, ordem, principal, resultado)
                values (1, 1, 1, 'LIDER', 0, true, 'N/A')
                """);
        jdbcTemplate.update("""
                insert into public.missao_participantes (id, missao_id, funcionario_id, papel, ordem, principal, resultado)
                values (2, 1, 2, 'INTEL', 1, false, 'N/A')
                """);
        jdbcTemplate.update("""
                insert into public.missao_eventos (id, missao_id, ocorrido_em, tipo, descricao)
                values (1, 1, TIMESTAMP WITH TIME ZONE '2026-04-02 09:15:00+00:00', 'INTEL', 'Threat perimeter mapped')
                """);
        jdbcTemplate.update("""
                insert into public.missao_eventos (id, missao_id, ocorrido_em, tipo, descricao)
                values (2, 1, TIMESTAMP WITH TIME ZONE '2026-04-02 10:45:00+00:00', 'CONTATO', 'Forward team established contact')
                """);
        jdbcTemplate.update("""
                insert into public.vw_resumo_missoes (missao_id, titulo, status, prioridade, local, ameaca, qtd_herois, qtd_eventos, primeira_acao, ultima_acao)
                values (1, 'Atlantic Shield', 'PLANEJADA', 'CRITICA', 'Atlantis', 'Omega Swarm', 2, 2,
                        TIMESTAMP WITH TIME ZONE '2026-04-02 09:15:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 10:45:00+00:00')
                """);
        jdbcTemplate.update("""
                insert into public.vw_resumo_missoes (missao_id, titulo, status, prioridade, local, ameaca, qtd_herois, qtd_eventos, primeira_acao, ultima_acao)
                values (2, 'Sky Bridge', 'EM_ANDAMENTO', 'ALTA', 'Orbital Gate', 'Omega Swarm', 1, 1,
                        TIMESTAMP WITH TIME ZONE '2026-04-02 10:30:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 11:00:00+00:00')
                """);
    }

    @Test
    void shouldExposeThreatResourceEntityLookupForMissionSchemas() throws Exception {
        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/missoes&operation=post&schemaType=request",
                String.class
        ));

        JsonNode ameacaUi = schema.path("properties").path("ameacaId").path("x-ui");
        JsonNode optionSource = ameacaUi.path("optionSource");
        assertEquals("entityLookup", ameacaUi.path("controlType").asText());
        assertEquals("/api/risk-intelligence/ameacas/option-sources/threat/options/filter",
                ameacaUi.path("endpoint").asText());
        assertEquals("threat", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/risk-intelligence/ameacas", optionSource.path("resourcePath").asText());
        assertEquals("threat", optionSource.path("entityKey").asText());
        assertEquals("status", optionSource.path("statusPropertyPath").asText());
        assertEquals("classe", optionSource.path("descriptionPropertyPaths").get(0).asText());
        assertEquals("nome", optionSource.path("searchPropertyPaths").get(0).asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
        assertTrue(optionSource.path("selectionPolicy").path("allowRetainInvalidExistingValue").asBoolean());
        assertEquals("status", optionSource.path("selectionPolicy").path("statusPropertyPath").asText());

        JsonNode threats = body(restTemplate.postForEntity(
                "/api/risk-intelligence/ameacas/option-sources/threat/options/filter?search=Omega",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, threats.path("content").size());
        JsonNode omega = threats.path("content").get(0);
        assertEquals(1, omega.path("id").asInt());
        assertEquals("Omega Swarm", omega.path("label").asText());
        assertEquals("INVASAO - Terra - 9", omega.path("extra").path("description").asText());
        assertEquals("EM_OBSERVACAO", omega.path("extra").path("status").asText());
        assertTrue(omega.path("extra").path("selectable").asBoolean());

        JsonNode blockedThreat = objectMapper.readTree(restTemplate.getForObject(
                "/api/risk-intelligence/ameacas/option-sources/threat/options/by-ids?ids=2",
                String.class
        ));
        assertEquals("Captured Syndicate", blockedThreat.get(0).path("label").asText());
        assertEquals("CAPTURADO", blockedThreat.get(0).path("extra").path("status").asText());
        assertFalse(blockedThreat.get(0).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldExposeMissionResourceEntityLookupForOperationalConsumers() throws Exception {
        JsonNode participanteSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/missao-participantes&operation=post&schemaType=request",
                String.class
        ));

        JsonNode missaoUi = participanteSchema.path("properties").path("missaoId").path("x-ui");
        JsonNode optionSource = missaoUi.path("optionSource");
        assertEquals("entityLookup", missaoUi.path("controlType").asText());
        assertEquals("/api/operations/missoes/option-sources/mission/options/filter",
                missaoUi.path("endpoint").asText());
        assertEquals("mission", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/operations/missoes", optionSource.path("resourcePath").asText());
        assertTrue(optionSource.path("filterField").isMissingNode() || optionSource.path("filterField").isNull());
        assertEquals("mission", optionSource.path("entityKey").asText());
        assertEquals("titulo", optionSource.path("labelPropertyPath").asText());
        assertEquals("status", optionSource.path("statusPropertyPath").asText());
        assertEquals("prioridade", optionSource.path("descriptionPropertyPaths").get(0).asText());
        assertEquals("local", optionSource.path("descriptionPropertyPaths").get(1).asText());
        assertEquals("titulo", optionSource.path("searchPropertyPaths").get(0).asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
        assertTrue(optionSource.path("selectionPolicy").path("allowedStatuses").toString().contains("PLANEJADA"));
        assertTrue(optionSource.path("selectionPolicy").path("blockedStatuses").toString().contains("CONCLUIDA"));

        JsonNode participanteFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/missao-participantes/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("inlineEntityLookup",
                participanteFilterSchema.path("properties").path("missaoId").path("x-ui").path("controlType").asText());
        assertEquals("mission",
                participanteFilterSchema.path("properties").path("missaoId").path("x-ui").path("optionSource").path("key").asText());
        assertEquals("inlineEntityLookup",
                participanteFilterSchema.path("properties").path("funcionarioId").path("x-ui").path("controlType").asText());
        assertEquals("employee",
                participanteFilterSchema.path("properties").path("funcionarioId").path("x-ui").path("optionSource").path("key").asText());
        assertEquals("/api/human-resources/funcionarios",
                participanteFilterSchema.path("properties").path("funcionarioId").path("x-ui").path("optionSource").path("resourcePath").asText());

        JsonNode incidenteSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/incidentes&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("entityLookup",
                incidenteSchema.path("properties").path("missaoId").path("x-ui").path("controlType").asText());

        JsonNode resumoFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/operations/vw-resumo-missoes/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("inlineEntityLookup",
                resumoFilterSchema.path("properties").path("missaoId").path("x-ui").path("controlType").asText());
        assertEquals("mission",
                resumoFilterSchema.path("properties").path("missaoIdsIn").path("x-ui").path("optionSource").path("key").asText());

        JsonNode usageSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/veiculo-missao-usos&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("entityLookup",
                usageSchema.path("properties").path("missaoId").path("x-ui").path("controlType").asText());

        JsonNode missions = body(restTemplate.postForEntity(
                "/api/operations/missoes/option-sources/mission/options/filter?search=Atlantic",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, missions.path("content").size());
        JsonNode atlantic = missions.path("content").get(0);
        assertEquals(1, atlantic.path("id").asInt());
        assertEquals("Atlantic Shield", atlantic.path("label").asText());
        assertEquals("CRITICA - Atlantis", atlantic.path("extra").path("description").asText());
        assertEquals("PLANEJADA", atlantic.path("extra").path("status").asText());
        assertTrue(atlantic.path("extra").path("selectable").asBoolean());

        JsonNode selectedMissions = objectMapper.readTree(restTemplate.getForObject(
                "/api/operations/missoes/option-sources/mission/options/by-ids?ids=1&ids=4",
                String.class
        ));
        assertEquals(2, selectedMissions.size());
        assertEquals("Atlantic Shield", selectedMissions.get(0).path("label").asText());
        assertTrue(selectedMissions.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Frozen Wall", selectedMissions.get(1).path("label").asText());
        assertEquals("CONCLUIDA", selectedMissions.get(1).path("extra").path("status").asText());
        assertFalse(selectedMissions.get(1).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldExposeTeamMembersSurfaceForOperationalCapacityNavigation() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.equipes",
                String.class
        ));
        assertEquals("operations.equipes", surfacesCatalog.path("resourceKey").asText());
        JsonNode membersSurface = findById(surfacesCatalog.path("surfaces"), "members");
        assertNotNull(membersSurface);
        assertEquals("READ_PROJECTION", membersSurface.path("kind").asText());
        assertEquals("ITEM", membersSurface.path("scope").asText());
        assertEquals("/api/operations/equipes/{id}/members", membersSurface.path("path").asText());
        assertEquals("COLLECTION", membersSurface.path("responseCardinality").asText());
        assertTrue(membersSurface.path("description").asText().contains("capacidade operacional"));
        assertEquals("operations.equipe-membros", membersSurface.path("relatedResource").path("childResourceKey").asText());
        assertEquals("/api/operations/equipe-membros", membersSurface.path("relatedResource").path("childResourcePath").asText());
        assertEquals("equipeId", membersSurface.path("relatedResource").path("childParentField").asText());
        assertTrue(membersSurface.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", membersSurface.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                membersSurface.path("relatedResource").path("childOperations").toString());

        JsonNode teamCapabilities = body(restTemplate.getForEntity(
                "/api/operations/equipes/1/capabilities",
                String.class
        ));
        assertTrue(findById(teamCapabilities.path("surfaces"), "members").path("availability").path("allowed").asBoolean());

        JsonNode members = body(restTemplate.getForEntity(
                "/api/operations/equipes/1/members",
                String.class
        ));
        assertEquals(2, members.path("data").size());
        assertEquals("Victor Stone", members.path("data").path(0).path("funcionarioNome").asText());
        assertEquals("SUPORTE", members.path("data").path(0).path("papel").asText());
        assertEquals("Diana Prince", members.path("data").path(1).path("funcionarioNome").asText());
        assertNotNull(findLinkHref(members, "schema"));
    }

    @Test
    void shouldExposeRescheduleSurfaceAndWorkflowActionsForMissoes() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.missoes",
                String.class
        ));
        assertEquals("operations.missoes", surfacesCatalog.path("resourceKey").asText());
        JsonNode reschedule = findById(surfacesCatalog.path("surfaces"), "reschedule");
        assertNotNull(reschedule);
        assertEquals("ITEM", reschedule.path("scope").asText());
        assertEquals("PATCH", reschedule.path("method").asText());
        assertEquals("/api/operations/missoes/{id}/reschedule", reschedule.path("path").asText());
        assertFalse(reschedule.path("availability").path("allowed").asBoolean());
        assertEquals("resource-context-required", reschedule.path("availability").path("reason").asText());
        JsonNode teamPlan = findById(surfacesCatalog.path("surfaces"), "team-plan");
        assertNotNull(teamPlan);
        assertEquals("ITEM", teamPlan.path("scope").asText());
        assertEquals("PATCH", teamPlan.path("method").asText());
        assertEquals("/api/operations/missoes/{id}/team-plan", teamPlan.path("path").asText());
        assertEquals("READ_PROJECTION", findById(surfacesCatalog.path("surfaces"), "summary").path("kind").asText());
        JsonNode teamSurface = findById(surfacesCatalog.path("surfaces"), "team");
        assertEquals("READ_PROJECTION", teamSurface.path("kind").asText());
        assertEquals("operations.missao-participantes", teamSurface.path("relatedResource").path("childResourceKey").asText());
        assertEquals("/api/operations/missao-participantes", teamSurface.path("relatedResource").path("childResourcePath").asText());
        assertEquals("missaoId", teamSurface.path("relatedResource").path("childParentField").asText());
        assertTrue(teamSurface.path("relatedResource").path("selectable").asBoolean());
        assertEquals("id", teamSurface.path("relatedResource").path("selectionKeyField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                teamSurface.path("relatedResource").path("childOperations").toString());
        JsonNode timelineSurface = findById(surfacesCatalog.path("surfaces"), "timeline");
        assertEquals("READ_PROJECTION", timelineSurface.path("kind").asText());
        assertEquals("operations.missao-eventos", timelineSurface.path("relatedResource").path("childResourceKey").asText());
        assertEquals("/api/operations/missao-eventos", timelineSurface.path("relatedResource").path("childResourcePath").asText());
        assertEquals("missaoId", timelineSurface.path("relatedResource").path("childParentField").asText());
        assertEquals("[\"FILTER\",\"LIST\",\"CREATE\",\"UPDATE\",\"DELETE\"]",
                timelineSurface.path("relatedResource").path("childOperations").toString());

        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=operations.missoes",
                String.class
        ));
        assertEquals(5, actionsCatalog.path("actions").size());
        assertNotNull(findById(actionsCatalog.path("actions"), "start"));
        assertNotNull(findById(actionsCatalog.path("actions"), "pause"));
        assertNotNull(findById(actionsCatalog.path("actions"), "resume"));
        assertNotNull(findById(actionsCatalog.path("actions"), "complete"));
        assertNotNull(findById(actionsCatalog.path("actions"), "fail"));

        JsonNode plannedCapabilities = body(restTemplate.getForEntity(
                "/api/operations/missoes/1/capabilities",
                String.class
        ));
        assertTrue(findById(plannedCapabilities.path("surfaces"), "reschedule").path("availability").path("allowed").asBoolean());
        assertTrue(findById(plannedCapabilities.path("surfaces"), "team-plan").path("availability").path("allowed").asBoolean());
        assertTrue(findById(plannedCapabilities.path("surfaces"), "summary").path("availability").path("allowed").asBoolean());
        assertTrue(findById(plannedCapabilities.path("surfaces"), "team").path("availability").path("allowed").asBoolean());
        assertTrue(findById(plannedCapabilities.path("surfaces"), "timeline").path("availability").path("allowed").asBoolean());
        assertTrue(findById(plannedCapabilities.path("actions"), "start").path("availability").path("allowed").asBoolean());
        assertFalse(findById(plannedCapabilities.path("actions"), "pause").path("availability").path("allowed").asBoolean());

        JsonNode teamPlanSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=patch&schemaType=request",
                String.class,
                "/api/operations/missoes/{id}/team-plan"
        ));
        JsonNode participantes = teamPlanSchema.path("properties").path("participantes");
        assertEquals("array", participantes.path("type").asText());
        assertEquals("array", participantes.path("x-ui").path("controlType").asText());
        assertEquals("cards", participantes.path("x-ui").path("array").path("mode").asText());
        assertEquals("removeFromPayload", participantes.path("x-ui").path("array").path("deleteMode").asText());
        assertEquals("#/components/schemas/PlanejarEquipeMissaoParticipanteDTO", participantes.path("x-ui").path("array").path("itemSchemaRef").asText());
        assertEquals("funcionarioId", participantes.path("x-ui").path("array").path("collectionValidation").path("uniqueBy").path(0).asText());
        assertEquals("principal", participantes.path("x-ui").path("array").path("collectionValidation").path("exactlyOne").path("field").asText());
        assertTrue(participantes.path("x-ui").path("array").path("collectionValidation").path("exactlyOne").path("value").isBoolean());
        assertTrue(participantes.path("x-ui").path("array").path("collectionValidation").path("exactlyOne").path("value").asBoolean());
        assertTrue(teamPlanSchema.path("components").path("schemas").has("PlanejarEquipeMissaoParticipanteDTO"));
        JsonNode itemFields = participantes.path("x-ui").path("array").path("itemSchema").path("fields");
        assertTrue(itemFields.isArray());
        assertNotNull(findByName(itemFields, "funcionarioId"));
        assertNotNull(findByName(itemFields, "papel"));
        assertEquals("toggle", findByName(itemFields, "principal").path("x-ui").path("controlType").asText());

        JsonNode summary = body(restTemplate.getForEntity(
                "/api/operations/missoes/1/summary",
                String.class
        ));
        assertEquals("Atlantic Shield", summary.path("data").path("titulo").asText());
        assertEquals(2, summary.path("data").path("qtdHerois").asInt());
        assertNotNull(findLinkHref(summary, "schema"));

        JsonNode team = body(restTemplate.getForEntity(
                "/api/operations/missoes/1/team",
                String.class
        ));
        assertEquals(2, team.path("data").size());
        assertEquals("Diana Prince", team.path("data").path(0).path("funcionarioNome").asText());

        JsonNode timeline = body(restTemplate.getForEntity(
                "/api/operations/missoes/1/timeline",
                String.class
        ));
        assertEquals(2, timeline.path("data").size());
        assertEquals("Forward team established contact", timeline.path("data").path(0).path("descricao").asText());

        JsonNode statusStats = body(restTemplate.postForEntity(
                "/api/operations/vw-resumo-missoes/stats/group-by",
                authorizedJson("""
                        {
                          "filter": {},
                          "field": "status",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """),
                String.class
        ));
        assertEquals("status", statusStats.path("data").path("field").asText());
        assertTrue(statusStats.path("data").path("buckets").size() > 0);

        JsonNode pausedActions = body(restTemplate.getForEntity(
                "/api/operations/missoes/3/actions",
                String.class
        ));
        assertTrue(findById(pausedActions.path("actions"), "resume").path("availability").path("allowed").asBoolean());
        assertTrue(findById(pausedActions.path("actions"), "fail").path("availability").path("allowed").asBoolean());
        assertFalse(findById(pausedActions.path("actions"), "start").path("availability").path("allowed").asBoolean());

        JsonNode completedSurfaces = body(restTemplate.getForEntity(
                "/api/operations/missoes/4/surfaces",
                String.class
        ));
        JsonNode completedReschedule = findById(completedSurfaces.path("surfaces"), "reschedule");
        assertFalse(completedReschedule.path("availability").path("allowed").asBoolean());
        assertEquals("resource-state-blocked", completedReschedule.path("availability").path("reason").asText());

        JsonNode itemEnvelope = body(restTemplate.getForEntity(
                "/api/operations/missoes/1",
                String.class
        ));
        assertNotNull(findLinkHref(itemEnvelope, "surfaces"));
        assertNotNull(findLinkHref(itemEnvelope, "actions"));
        assertNotNull(findLinkHref(itemEnvelope, "capabilities"));
    }

    @Test
    void shouldExecuteRescheduleAndWorkflowTransitionsForMissoes() throws Exception {
        ResponseEntity<String> rescheduleResponse = restTemplate.exchange(
                "/api/operations/missoes/1/reschedule",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "local": "Atlantic Gate",
                          "inicioPrev": "2026-04-02T11:00:00Z",
                          "fimPrev": "2026-04-02T20:00:00Z",
                          "objetivo": "Protect the Atlantic evacuation corridor"
                        }
                        """),
                String.class
        );
        JsonNode rescheduleBody = body(rescheduleResponse);
        assertEquals("Atlantic Gate", rescheduleBody.path("data").path("local").asText());
        assertEquals("Protect the Atlantic evacuation corridor", rescheduleBody.path("data").path("objetivo").asText());

        ResponseEntity<String> teamPlanResponse = restTemplate.exchange(
                "/api/operations/missoes/1/team-plan",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "participantes": [
                            {
                              "id": 2,
                              "funcionarioId": 2,
                              "funcionarioNome": "Victor Stone",
                              "papel": "INTEL",
                              "principal": true
                            }
                          ]
                        }
                        """),
                String.class
        );
        JsonNode teamPlanBody = body(teamPlanResponse);
        assertEquals(1, teamPlanBody.path("data").path("id").asInt());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.missao_participantes where missao_id = 1",
                Integer.class
        ));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select funcionario_id from public.missao_participantes where missao_id = 1 and principal = true",
                Integer.class
        ));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select ordem from public.missao_participantes where id = 2",
                Integer.class
        ));

        ResponseEntity<String> invalidTeamPlan = restTemplate.exchange(
                "/api/operations/missoes/1/team-plan",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "participantes": [
                            {
                              "id": 2,
                              "funcionarioId": 2,
                              "papel": "INTEL",
                              "principal": false
                            }
                          ]
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidTeamPlan.getStatusCode());

        ResponseEntity<String> startResponse = restTemplate.exchange(
                "/api/operations/missoes/1/actions/start",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Launch window cleared",
                          "ocorridoEm": "2026-04-02T11:15:00Z"
                        }
                        """),
                String.class
        );
        JsonNode startBody = body(startResponse);
        assertEquals("PLANEJADA", startBody.path("data").path("statusAnterior").asText());
        assertEquals("EM_ANDAMENTO", startBody.path("data").path("statusAtual").asText());
        assertEquals("EM_ANDAMENTO", jdbcTemplate.queryForObject(
                "select status from public.missoes where id = 1",
                String.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.missoes where id = 1 and inicio_real is not null",
                Integer.class
        ));

        ResponseEntity<String> duplicateStart = restTemplate.exchange(
                "/api/operations/missoes/1/actions/start",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Duplicate start"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateStart.getStatusCode());

        ResponseEntity<String> pauseResponse = restTemplate.exchange(
                "/api/operations/missoes/2/actions/pause",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Weather hold"
                        }
                        """),
                String.class
        );
        JsonNode pauseBody = body(pauseResponse);
        assertEquals("EM_ANDAMENTO", pauseBody.path("data").path("statusAnterior").asText());
        assertEquals("PAUSADA", pauseBody.path("data").path("statusAtual").asText());

        ResponseEntity<String> failResponse = restTemplate.exchange(
                "/api/operations/missoes/2/actions/fail",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Bridge collapse",
                          "ocorridoEm": "2026-04-02T12:30:00Z"
                        }
                        """),
                String.class
        );
        JsonNode failBody = body(failResponse);
        assertEquals("PAUSADA", failBody.path("data").path("statusAnterior").asText());
        assertEquals("FALHOU", failBody.path("data").path("statusAtual").asText());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.missoes where id = 2 and fim_real is not null",
                Integer.class
        ));

        ResponseEntity<String> resumeResponse = restTemplate.exchange(
                "/api/operations/missoes/3/actions/resume",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Perimeter restored"
                        }
                        """),
                String.class
        );
        JsonNode resumeBody = body(resumeResponse);
        assertEquals("PAUSADA", resumeBody.path("data").path("statusAnterior").asText());
        assertEquals("EM_ANDAMENTO", resumeBody.path("data").path("statusAtual").asText());

        ResponseEntity<String> completeResponse = restTemplate.exchange(
                "/api/operations/missoes/3/actions/complete",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Area secured",
                          "ocorridoEm": "2026-04-03T15:45:00Z"
                        }
                        """),
                String.class
        );
        JsonNode completeBody = body(completeResponse);
        assertEquals("EM_ANDAMENTO", completeBody.path("data").path("statusAnterior").asText());
        assertEquals("CONCLUIDA", completeBody.path("data").path("statusAtual").asText());
        assertEquals("CONCLUIDA", jdbcTemplate.queryForObject(
                "select status from public.missoes where id = 3",
                String.class
        ));

        ResponseEntity<String> invalidReschedule = restTemplate.exchange(
                "/api/operations/missoes/4/reschedule",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "local": "Arctic North",
                          "inicioPrev": "2026-04-04T11:00:00Z",
                          "fimPrev": "2026-04-04T18:00:00Z",
                          "objetivo": "Should not be allowed"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, invalidReschedule.getStatusCode());
    }

    @Test
    void shouldBlockMissionWorkflowActionWhenGovernedPolicyIsApplied() {
        when(workflowActionPolicyResolver.resolveAppliedPolicy("operations.missoes:pause"))
                .thenReturn(Optional.of(new DomainRuleWorkflowActionPolicy(
                        "operations.missoes:pause",
                        "operations.missoes",
                        "pause",
                        List.of("EM_ANDAMENTO"),
                        "Pausa bloqueada por decisao operacional governada."
                )));

        ResponseEntity<String> pauseResponse = restTemplate.exchange(
                "/api/operations/missoes/2/actions/pause",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Weather hold"
                        }
                        """),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, pauseResponse.getStatusCode());
        assertEquals("EM_ANDAMENTO", jdbcTemplate.queryForObject(
                "select status from public.missoes where id = 2",
                String.class
        ));
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

    private JsonNode findByName(JsonNode items, String name) {
        for (JsonNode item : items) {
            if (name.equals(item.path("name").asText())) {
                return item;
            }
        }
        return null;
    }

    private String findLinkHref(JsonNode envelope, String rel) {
        JsonNode links = envelope.path("_links");
        if (!links.isObject()) {
            return null;
        }

        JsonNode halLink = links.path(rel);
        return halLink.isObject() ? halLink.path("href").asText(null) : null;
    }
}
