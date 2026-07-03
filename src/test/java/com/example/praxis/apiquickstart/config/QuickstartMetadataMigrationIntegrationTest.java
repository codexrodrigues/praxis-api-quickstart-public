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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_metadata_migration_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_metadata_migration_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class QuickstartMetadataMigrationIntegrationTest {

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
        jdbcTemplate.execute("drop table if exists public.vw_perfil_heroi");
        jdbcTemplate.execute("drop table if exists public.vw_analytics_folha_pagamento");
        jdbcTemplate.execute("drop table if exists public.missao_participantes");
        jdbcTemplate.execute("drop table if exists public.missoes");
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("drop table if exists public.cargos");
        jdbcTemplate.execute("drop table if exists public.departamentos");

        jdbcTemplate.execute("""
                create table public.cargos (
                    id integer primary key,
                    nome varchar(200) not null,
                    nivel varchar(100) not null,
                    descricao varchar(500),
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
                create table public.funcionarios (
                    id integer generated by default as identity primary key,
                    nome_completo varchar(200) not null,
                    cpf varchar(11) not null,
                    data_nascimento date not null,
                    email varchar(200) not null,
                    telefone varchar(30) not null,
                    salario numeric(19, 2) not null,
                    data_admissao date not null,
                    ativo boolean not null,
                    cargo_id integer not null,
                    departamento_id integer not null,
                    foto_perfil_url varchar(300),
                    estado_civil varchar(50),
                    pais_nascimento varchar(200),
                    cidade_nascimento varchar(200)
                )
                """);

        jdbcTemplate.execute("""
                create table public.vw_perfil_heroi (
                    funcionario_id integer primary key,
                    avatar_url varchar(300),
                    nome_completo varchar(200),
                    codinome varchar(200),
                    universo varchar(200),
                    exposicao_publica boolean,
                    cargo varchar(200),
                    departamento varchar(200),
                    score_publico integer,
                    score_governamental integer,
                    score_medio numeric(19, 2),
                    habilidades varchar(500),
                    equipe_principal varchar(200),
                    base_principal varchar(200)
                )
                """);

        jdbcTemplate.execute("""
                create table public.vw_analytics_folha_pagamento (
                    folha_pagamento_id integer primary key,
                    funcionario_id integer,
                    nome_completo varchar(200),
                    codinome varchar(200),
                    universo varchar(200),
                    exposicao_publica boolean,
                    cargo_id integer,
                    cargo varchar(200),
                    departamento_id integer,
                    departamento varchar(200),
                    equipe_id integer,
                    equipe varchar(200),
                    papel_equipe varchar(200),
                    base_id integer,
                    base varchar(200),
                    tipo_base varchar(200),
                    sigilo_base varchar(200),
                    ano integer,
                    mes integer,
                    competencia date,
                    data_pagamento date,
                    salario_bruto numeric(19, 2),
                    total_descontos numeric(19, 2),
                    salario_liquido numeric(19, 2),
                    qtd_eventos bigint,
                    qtd_proventos bigint,
                    qtd_descontos bigint,
                    qtd_adicionais bigint,
                    qtd_tipos_evento bigint,
                    valor_proventos numeric(19, 2),
                    valor_descontos_eventos numeric(19, 2),
                    valor_adicionais numeric(19, 2),
                    saldo_eventos numeric(19, 2),
                    saldo_liquido_vs_bruto numeric(19, 2),
                    pct_desconto numeric(19, 4),
                    pct_liquido numeric(19, 4),
                    pct_adicionais_sobre_bruto numeric(19, 4),
                    pct_eventos_desconto_sobre_bruto numeric(19, 4),
                    faixa_salario_bruto varchar(200),
                    faixa_salario_liquido varchar(200),
                    faixa_pct_desconto varchar(200),
                    faixa_valor_adicionais varchar(200),
                    payroll_profile varchar(200),
                    composicao_folha varchar(200),
                    eventos_descricao varchar(500)
                )
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

        jdbcTemplate.update("""
                insert into public.cargos (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values (1, 'Analista', 'Senior', 'Analista principal', 8000.00, 15000.00)
                """);
        jdbcTemplate.update("""
                insert into public.departamentos (id, nome, codigo, responsavel_id)
                values (1, 'Financeiro', 'FIN', null)
                """);
        jdbcTemplate.update("""
                insert into public.funcionarios (
                    id, nome_completo, cpf, data_nascimento, email, telefone, salario, data_admissao, ativo, cargo_id,
                    departamento_id, foto_perfil_url, estado_civil, pais_nascimento, cidade_nascimento
                ) values (
                    1, 'Bruce Wayne', '12345678901', DATE '1985-02-19', 'bruce@wayne.com', '+5511999999999',
                    10000.00, DATE '2024-01-10', true, 1, 1, 'https://img.example/bruce.png', 'SOLTEIRO', 'Brasil', 'Gotham'
                )
                """);
        jdbcTemplate.update("""
                insert into public.vw_perfil_heroi (
                    funcionario_id, avatar_url, nome_completo, codinome, universo, exposicao_publica, cargo, departamento,
                    score_publico, score_governamental, score_medio, habilidades, equipe_principal, base_principal
                ) values (
                    1, 'https://img.example/bruce.png', 'Bruce Wayne', 'Batman', 'DC', true, 'Analista', 'Financeiro',
                    98, 91, 94.50, 'Investigacao, estrategia, tecnologia', 'Liga', 'Batcaverna'
                )
                """);
        jdbcTemplate.update("""
                insert into public.vw_analytics_folha_pagamento (
                    folha_pagamento_id, funcionario_id, nome_completo, codinome, universo, exposicao_publica,
                    cargo_id, cargo, departamento_id, departamento, equipe_id, equipe, papel_equipe, base_id, base,
                    tipo_base, sigilo_base, ano, mes, competencia, data_pagamento, salario_bruto, total_descontos,
                    salario_liquido, qtd_eventos, qtd_proventos, qtd_descontos, qtd_adicionais, qtd_tipos_evento,
                    valor_proventos, valor_descontos_eventos, valor_adicionais, saldo_eventos, saldo_liquido_vs_bruto,
                    pct_desconto, pct_liquido, pct_adicionais_sobre_bruto, pct_eventos_desconto_sobre_bruto,
                    faixa_salario_bruto, faixa_salario_liquido, faixa_pct_desconto, faixa_valor_adicionais,
                    payroll_profile, composicao_folha, eventos_descricao
                ) values (
                    10, 1, 'Bruce Wayne', 'Batman', 'DC', true,
                    1, 'Analista', 1, 'Financeiro', 100, 'Liga', 'Lider', 1000, 'Batcaverna',
                    'SECRETA', 'ALTO', 2026, 3, DATE '2026-03-01', DATE '2026-03-30', 10000.00, 1500.00,
                    8500.00, 5, 3, 1, 1, 4,
                    11000.00, 1500.00, 500.00, 10000.00, -1500.00,
                    15.0000, 85.0000, 5.0000, 15.0000,
                    '9000-12000', '8000-9000', '10-20', '0-1000',
                    'Executivo', 'FIXO+VARIAVEL', 'Salario, bonus, desconto'
                )
                """);
        jdbcTemplate.update("""
                insert into public.missoes (id, titulo, objetivo, prioridade, status, local, ameaca_id, inicio_prev, fim_prev, inicio_real, fim_real)
                values (20, 'Gotham Shield', 'Conter ameaça urbana em Gotham', 'CRITICA', 'CONCLUIDA', 'Gotham',
                        null,
                        TIMESTAMP WITH TIME ZONE '2026-04-02 09:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 18:00:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 09:15:00+00:00',
                        TIMESTAMP WITH TIME ZONE '2026-04-02 17:40:00+00:00')
                """);
        jdbcTemplate.update("""
                insert into public.missao_participantes (id, missao_id, funcionario_id, papel, ordem, principal, resultado)
                values (30, 20, 1, 'LIDER', 0, true, 'OK')
                """);
    }

    @Test
    void shouldExposeProfileSurfaceAndCapabilitiesForMigratedFuncionario() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.funcionarios",
                String.class
        ));
        assertEquals("human-resources.funcionarios", surfacesCatalog.path("resourceKey").asText());
        JsonNode profileSurface = findById(surfacesCatalog.path("surfaces"), "profile");
        assertNotNull(profileSurface);
        assertEquals("PATCH", profileSurface.path("method").asText());
        assertEquals("PARTIAL_FORM", profileSurface.path("kind").asText());
        assertEquals("/api/human-resources/funcionarios/{id}/profile", profileSurface.path("path").asText());
        assertFalse(profileSurface.path("availability").path("allowed").asBoolean());
        assertEquals("resource-context-required", profileSurface.path("availability").path("reason").asText());
        JsonNode heroProfileSurface = findById(surfacesCatalog.path("surfaces"), "hero-profile");
        assertNotNull(heroProfileSurface);
        assertEquals("GET", heroProfileSurface.path("method").asText());
        assertEquals("READ_PROJECTION", heroProfileSurface.path("kind").asText());
        assertEquals("/api/human-resources/funcionarios/{id}/hero-profile", heroProfileSurface.path("path").asText());
        assertEquals("OBJECT", heroProfileSurface.path("responseCardinality").asText());
        JsonNode payrollHistorySurface = findById(surfacesCatalog.path("surfaces"), "payroll-history");
        assertNotNull(payrollHistorySurface);
        assertEquals("READ_PROJECTION", payrollHistorySurface.path("kind").asText());
        assertEquals("/api/human-resources/funcionarios/{id}/payroll-history", payrollHistorySurface.path("path").asText());
        assertEquals("COLLECTION", payrollHistorySurface.path("responseCardinality").asText());
        assertTrue(payrollHistorySurface.path("description").asText().contains("folha de pagamento"));
        assertTrue(payrollHistorySurface.path("tags").toString().contains("\"folha-de-pagamento\""));
        JsonNode missionParticipationsSurface = findById(surfacesCatalog.path("surfaces"), "mission-participations");
        assertNotNull(missionParticipationsSurface);
        assertEquals("READ_PROJECTION", missionParticipationsSurface.path("kind").asText());
        assertEquals("/api/human-resources/funcionarios/{id}/mission-participations", missionParticipationsSurface.path("path").asText());
        assertEquals("COLLECTION", missionParticipationsSurface.path("responseCardinality").asText());
        assertTrue(missionParticipationsSurface.path("description").asText().contains("missões"));

        JsonNode itemSurfaces = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1/surfaces",
                String.class
        ));
        JsonNode contextualProfile = findById(itemSurfaces.path("surfaces"), "profile");
        assertNotNull(contextualProfile);
        assertTrue(contextualProfile.path("availability").path("allowed").asBoolean());
        assertTrue(findById(itemSurfaces.path("surfaces"), "hero-profile").path("availability").path("allowed").asBoolean());
        assertTrue(findById(itemSurfaces.path("surfaces"), "payroll-history").path("availability").path("allowed").asBoolean());
        assertTrue(findById(itemSurfaces.path("surfaces"), "mission-participations").path("availability").path("allowed").asBoolean());

        JsonNode itemCapabilities = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1/capabilities",
                String.class
        ));
        assertTrue(itemCapabilities.path("canonicalOperations").path("update").asBoolean());
        assertTrue(itemCapabilities.path("operations").path("view").path("supported").asBoolean());
        assertEquals("GET", itemCapabilities.path("operations").path("view").path("preferredMethod").asText());
        assertTrue(itemCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertEquals("PUT", itemCapabilities.path("operations").path("edit").path("preferredMethod").asText());
        assertNotNull(findById(itemCapabilities.path("surfaces"), "profile"));
        assertNotNull(findById(itemCapabilities.path("surfaces"), "hero-profile"));
        assertNotNull(findById(itemCapabilities.path("surfaces"), "payroll-history"));
        assertNotNull(findById(itemCapabilities.path("surfaces"), "mission-participations"));
        assertEquals(0, itemCapabilities.path("actions").size());

        JsonNode heroProfile = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1/hero-profile",
                String.class
        ));
        assertEquals("Batman", heroProfile.path("data").path("codinome").asText());
        assertEquals("https://img.example/bruce.png", heroProfile.path("data").path("avatarUrl").asText());
        assertNotNull(findLinkHref(heroProfile, "schema"));

        JsonNode heroProfileSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=get&schemaType=response",
                String.class,
                "/api/human-resources/funcionarios/{id}/hero-profile"
        ));
        JsonNode heroAvatarUi = heroProfileSchema.path("properties").path("avatarUrl").path("x-ui");
        assertEquals("avatar", heroAvatarUi.path("controlType").asText());
        assertEquals("Foto", heroAvatarUi.path("label").asText());

        JsonNode payrollHistory = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1/payroll-history",
                String.class
        ));
        assertEquals(1, payrollHistory.path("data").size());
        assertEquals(10, payrollHistory.path("data").path(0).path("folhaPagamentoId").asInt());
        assertNotNull(findLinkHref(payrollHistory, "schema"));

        JsonNode missionParticipations = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1/mission-participations",
                String.class
        ));
        assertEquals(1, missionParticipations.path("data").size());
        assertEquals("Gotham Shield", missionParticipations.path("data").path(0).path("missaoTitulo").asText());
        assertEquals("LIDER", missionParticipations.path("data").path(0).path("papel").asText());
        assertNotNull(findLinkHref(missionParticipations, "schema"));

        ResponseEntity<String> missingActionsCatalog = restTemplate.getForEntity(
                "/schemas/actions?resource=human-resources.funcionarios",
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, missingActionsCatalog.getStatusCode());

        JsonNode patchSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=patch&schemaType=request",
                String.class,
                "/api/human-resources/funcionarios/{id}/profile"
        ));
        assertTrue(patchSchema.path("properties").has("nomeCompleto"));
        assertEquals("Novo nome de exibição pública do colaborador.",
                patchSchema.path("properties").path("nomeCompleto").path("x-ui").path("helpText").asText());
        assertTrue(patchSchema.path("properties").has("email"));
        assertEquals("Novo endereço de e-mail operacional.",
                patchSchema.path("properties").path("email").path("x-ui").path("helpText").asText());
        assertTrue(patchSchema.path("properties").has("telefone"));
        assertEquals("Novo telefone de contato de emergência.",
                patchSchema.path("properties").path("telefone").path("x-ui").path("helpText").asText());
        assertFalse(patchSchema.path("properties").has("salario"));
    }

    @Test
    void shouldExposeSeparatedCreateAndUpdateSchemasForMigratedCrudExamples() throws Exception {
        JsonNode cargoCreateSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                "/api/human-resources/cargos"
        ));
        assertFalse(cargoCreateSchema.path("properties").has("id"));
        assertTrue(cargoCreateSchema.path("properties").has("nome"));
        assertTrue(cargoCreateSchema.path("properties").has("nivel"));

        JsonNode cargoUpdateSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=put&schemaType=request",
                String.class,
                "/api/human-resources/cargos/{id}"
        ));
        assertFalse(cargoUpdateSchema.path("properties").has("id"));
        assertTrue(cargoUpdateSchema.path("properties").has("nome"));
        assertTrue(cargoUpdateSchema.path("properties").has("nivel"));

        JsonNode funcionarioCreateSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                "/api/human-resources/funcionarios"
        ));
        assertFalse(funcionarioCreateSchema.path("properties").has("id"));
        assertFalse(funcionarioCreateSchema.path("properties").has("avatarUrl"));
        assertFalse(funcionarioCreateSchema.path("properties").has("cargoNome"));
        assertFalse(funcionarioCreateSchema.path("properties").has("departamentoNome"));
        assertTrue(funcionarioCreateSchema.path("properties").has("cargoId"));
        assertTrue(funcionarioCreateSchema.path("properties").has("departamentoId"));

        JsonNode funcionarioUpdateSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=put&schemaType=request",
                String.class,
                "/api/human-resources/funcionarios/{id}"
        ));
        assertFalse(funcionarioUpdateSchema.path("properties").has("id"));
        assertFalse(funcionarioUpdateSchema.path("properties").has("avatarUrl"));
        assertFalse(funcionarioUpdateSchema.path("properties").has("cargoNome"));
        assertFalse(funcionarioUpdateSchema.path("properties").has("departamentoNome"));
        assertTrue(funcionarioUpdateSchema.path("properties").has("salario"));

        JsonNode funcionarioViewSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/human-resources/funcionarios/%7Bid%7D&operation=get&schemaType=response",
                String.class
        ));
        JsonNode avatarUi = funcionarioViewSchema.path("properties").path("avatarUrl").path("x-ui");
        assertEquals("avatar", avatarUi.path("controlType").asText());
        assertEquals("Foto", avatarUi.path("label").asText());
        assertTrue(avatarUi.path("helpText").isMissingNode() || avatarUi.path("helpText").asText().isBlank());
        assertFalse(avatarUi.path("formHidden").asBoolean(false));
        assertFalse(avatarUi.path("tableHidden").asBoolean(false));

        JsonNode photoUrlUi = funcionarioViewSchema.path("properties").path("fotoPerfilUrl").path("x-ui");
        assertTrue(photoUrlUi.path("formHidden").asBoolean(false));
        assertTrue(photoUrlUi.path("tableHidden").asBoolean(false));
    }

    @Test
    void shouldPatchFuncionarioProfileWithoutIntroducingWorkflowSecurity() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/human-resources/funcionarios/1/profile",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "nomeCompleto": "Bruce Thomas Wayne",
                          "email": "batman@wayne.com",
                          "telefone": "+5511988887777",
                          "fotoPerfilUrl": "https://img.example/batman.png",
                          "estadoCivil": "CASADO"
                        }
                        """),
                String.class
        );

        JsonNode body = body(response);
        assertEquals("Bruce Thomas Wayne", body.path("data").path("nomeCompleto").asText());
        assertEquals("batman@wayne.com", body.path("data").path("email").asText());
        assertEquals("CASADO", body.path("data").path("estadoCivil").asText());

        assertEquals("Bruce Thomas Wayne", jdbcTemplate.queryForObject(
                "select nome_completo from public.funcionarios where id = 1",
                String.class
        ));
        assertEquals("batman@wayne.com", jdbcTemplate.queryForObject(
                "select email from public.funcionarios where id = 1",
                String.class
        ));
    }

    @Test
    void shouldExposeReadOnlyCapabilitiesForMigratedAnalyticsView() throws Exception {
        JsonNode readOnlySurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.vw-analytics-folha-pagamento",
                String.class
        ));
        assertNotNull(findById(readOnlySurfaces.path("surfaces"), "detail"));
        assertNull(findById(readOnlySurfaces.path("surfaces"), "create"));

        JsonNode readOnlyCapabilities = body(restTemplate.getForEntity(
                "/api/human-resources/vw-analytics-folha-pagamento/capabilities",
                String.class
        ));
        assertFalse(readOnlyCapabilities.path("canonicalOperations").path("create").asBoolean());
        assertFalse(readOnlyCapabilities.path("canonicalOperations").path("update").asBoolean());
        assertFalse(readOnlyCapabilities.path("canonicalOperations").path("delete").asBoolean());
        assertTrue(readOnlyCapabilities.path("canonicalOperations").path("filter").asBoolean());
        assertFalse(readOnlyCapabilities.path("operations").path("create").path("supported").asBoolean());
        assertTrue(readOnlyCapabilities.path("operations").path("view").path("supported").asBoolean());
        assertFalse(readOnlyCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertFalse(readOnlyCapabilities.path("operations").path("delete").path("supported").asBoolean());
        assertEquals(0, readOnlyCapabilities.path("actions").size());
    }

    @Test
    void shouldExposeHypermediaDiscoveryLinksForFuncionarioCollectionAndItem() throws Exception {
        JsonNode collectionEnvelope = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/all",
                String.class
        ));
        assertTrue(collectionEnvelope.has("_links"));
        assertTrue(collectionEnvelope.path("_links").isObject());
        assertTrue(collectionEnvelope.path("data").path(0).path("_links").isObject());

        String collectionSurfacesHref = findLinkHref(collectionEnvelope, "surfaces");
        String collectionActionsHref = findLinkHref(collectionEnvelope, "actions");
        String collectionCapabilitiesHref = findLinkHref(collectionEnvelope, "capabilities");

        assertNotNull(collectionSurfacesHref);
        assertNull(collectionActionsHref);
        assertNotNull(collectionCapabilitiesHref);

        JsonNode collectionSurfaces = body(getHref(collectionSurfacesHref));
        assertNotNull(findById(collectionSurfaces.path("surfaces"), "create"));
        assertNotNull(findById(collectionSurfaces.path("surfaces"), "detail"));

        JsonNode collectionCapabilities = body(getHref(collectionCapabilitiesHref));
        assertTrue(collectionCapabilities.path("canonicalOperations").path("create").asBoolean());
        assertTrue(collectionCapabilities.path("canonicalOperations").path("update").asBoolean());
        assertTrue(collectionCapabilities.path("operations").path("create").path("supported").asBoolean());
        assertEquals("POST", collectionCapabilities.path("operations").path("create").path("preferredMethod").asText());
        assertTrue(collectionCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertEquals(0, collectionCapabilities.path("actions").size());

        JsonNode itemEnvelope = body(restTemplate.getForEntity(
                "/api/human-resources/funcionarios/1",
                String.class
        ));
        assertTrue(itemEnvelope.has("_links"));
        assertTrue(itemEnvelope.path("_links").isObject());

        String itemSurfacesHref = findLinkHref(itemEnvelope, "surfaces");
        String itemActionsHref = findLinkHref(itemEnvelope, "actions");
        String itemCapabilitiesHref = findLinkHref(itemEnvelope, "capabilities");

        assertNotNull(itemSurfacesHref);
        assertNull(itemActionsHref);
        assertNotNull(itemCapabilitiesHref);

        JsonNode itemSurfaces = body(getHref(itemSurfacesHref));
        JsonNode profileSurface = findById(itemSurfaces.path("surfaces"), "profile");
        assertNotNull(profileSurface);
        assertTrue(profileSurface.path("availability").path("allowed").asBoolean());

        JsonNode profileSchema = body(getHref(profileSurface.path("schemaUrl").asText()));
        assertTrue(profileSchema.path("properties").has("nomeCompleto"));
        assertTrue(profileSchema.path("properties").has("email"));
        assertFalse(profileSchema.path("properties").has("salario"));

        JsonNode itemCapabilities = body(getHref(itemCapabilitiesHref));
        assertTrue(itemCapabilities.path("operations").path("view").path("supported").asBoolean());
        assertTrue(itemCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertNotNull(findById(itemCapabilities.path("surfaces"), "profile"));
        assertEquals(0, itemCapabilities.path("actions").size());
    }

    @Test
    void shouldExposeHypermediaDiscoveryLinksForReadOnlyAnalyticsView() throws Exception {
        JsonNode collectionEnvelope = body(restTemplate.getForEntity(
                "/api/human-resources/vw-analytics-folha-pagamento/all",
                String.class
        ));
        assertTrue(collectionEnvelope.has("_links"));
        assertTrue(collectionEnvelope.path("_links").isObject());
        assertTrue(collectionEnvelope.path("data").path(0).path("_links").isObject());

        String collectionSurfacesHref = findLinkHref(collectionEnvelope, "surfaces");
        String collectionActionsHref = findLinkHref(collectionEnvelope, "actions");
        String collectionCapabilitiesHref = findLinkHref(collectionEnvelope, "capabilities");
        String createHref = findLinkHref(collectionEnvelope, "create");

        assertNotNull(collectionSurfacesHref);
        assertNull(collectionActionsHref);
        assertNotNull(collectionCapabilitiesHref);
        assertNull(createHref);

        JsonNode collectionSurfaces = body(getHref(collectionSurfacesHref));
        assertNotNull(findById(collectionSurfaces.path("surfaces"), "list"));
        assertNotNull(findById(collectionSurfaces.path("surfaces"), "detail"));
        assertNull(findById(collectionSurfaces.path("surfaces"), "create"));

        JsonNode collectionCapabilities = body(getHref(collectionCapabilitiesHref));
        assertFalse(collectionCapabilities.path("canonicalOperations").path("create").asBoolean());
        assertFalse(collectionCapabilities.path("canonicalOperations").path("update").asBoolean());
        assertFalse(collectionCapabilities.path("canonicalOperations").path("delete").asBoolean());
        assertFalse(collectionCapabilities.path("operations").path("create").path("supported").asBoolean());
        assertTrue(collectionCapabilities.path("operations").path("view").path("supported").asBoolean());
        assertFalse(collectionCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertFalse(collectionCapabilities.path("operations").path("delete").path("supported").asBoolean());
        assertEquals(0, collectionCapabilities.path("actions").size());

        JsonNode itemEnvelope = body(restTemplate.getForEntity(
                "/api/human-resources/vw-analytics-folha-pagamento/10",
                String.class
        ));
        assertTrue(itemEnvelope.has("_links"));
        assertTrue(itemEnvelope.path("_links").isObject());

        String itemSurfacesHref = findLinkHref(itemEnvelope, "surfaces");
        String itemActionsHref = findLinkHref(itemEnvelope, "actions");
        String itemCapabilitiesHref = findLinkHref(itemEnvelope, "capabilities");
        String itemUpdateHref = findLinkHref(itemEnvelope, "update");
        String itemDeleteHref = findLinkHref(itemEnvelope, "delete");

        assertNotNull(itemSurfacesHref);
        assertNull(itemActionsHref);
        assertNotNull(itemCapabilitiesHref);
        assertNull(itemUpdateHref);
        assertNull(itemDeleteHref);

        JsonNode itemSurfaces = body(getHref(itemSurfacesHref));
        JsonNode detailSurface = findById(itemSurfaces.path("surfaces"), "detail");
        assertNotNull(detailSurface);

        JsonNode detailSchema = body(getHref(detailSurface.path("schemaUrl").asText()));
        assertTrue(detailSchema.path("properties").has("nomeCompleto"));
        assertTrue(detailSchema.path("properties").has("salarioLiquido"));

        JsonNode itemCapabilities = body(getHref(itemCapabilitiesHref));
        assertTrue(itemCapabilities.path("operations").path("view").path("supported").asBoolean());
        assertFalse(itemCapabilities.path("operations").path("edit").path("supported").asBoolean());
        assertNotNull(findById(itemCapabilities.path("surfaces"), "detail"));
        assertEquals(0, itemCapabilities.path("actions").size());
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

    private String findLinkHref(JsonNode envelope, String rel) {
        JsonNode links = envelope.path("_links");
        if (!links.isObject()) {
            return null;
        }

        JsonNode halLink = links.path(rel);
        return halLink.isObject() ? halLink.path("href").asText(null) : null;
    }

    private ResponseEntity<String> getHref(String href) {
        return restTemplate.getForEntity(URI.create(href), String.class);
    }
}
