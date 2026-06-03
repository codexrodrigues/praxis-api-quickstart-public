package com.example.praxis.apiquickstart.operations;

import com.example.praxis.apiquickstart.security.AbstractAuthenticatedHttpTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quickstart_operations_stats_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "config.datasource.url=jdbc:h2:mem:quickstart_operations_stats_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "config.datasource.driver-class-name=org.h2.Driver",
        "config.datasource.username=sa",
        "config.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
class OperationsMissionStatsSmokeHttpTest extends AbstractAuthenticatedHttpTest {

    @Autowired
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedStatsTables() {
        jdbcTemplate.execute("drop table if exists public.missao_eventos");
        jdbcTemplate.execute("drop table if exists public.missao_participantes");

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

        jdbcTemplate.update("""
                insert into public.missao_participantes (id, missao_id, funcionario_id, papel, ordem, principal, resultado)
                values (1, 1, 10, 'LIDER', 0, true, 'OK')
                """);
        jdbcTemplate.update("""
                insert into public.missao_participantes (id, missao_id, funcionario_id, papel, ordem, principal, resultado)
                values (2, 1, 11, 'INTEL', 1, false, 'OK')
                """);
        jdbcTemplate.update("""
                insert into public.missao_eventos (id, missao_id, ocorrido_em, tipo, descricao)
                values (1, 1, TIMESTAMP WITH TIME ZONE '2026-04-02 09:15:00+00:00', 'INTEL', 'Threat perimeter mapped')
                """);
        jdbcTemplate.update("""
                insert into public.missao_eventos (id, missao_id, ocorrido_em, tipo, descricao)
                values (2, 1, TIMESTAMP WITH TIME ZONE '2026-04-02 10:45:00+00:00', 'CONTATO', 'Forward team established contact')
                """);
    }

    @Test
    void shouldServeMissionParticipantGroupByStats() throws Exception {
        AuthCookies auth = loginAsAdmin();

        postJson(auth,
                "/api/operations/missao-participantes/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "papel",
                          "metric": { "operation": "COUNT", "alias": "participants" },
                          "limit": 10,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("papel"),
                jsonPath("$.data.buckets[0].label").exists(),
                jsonPath("$.data.buckets[0].count").isNumber());
    }

    @Test
    void shouldServeMissionEventTimeSeriesStats() throws Exception {
        AuthCookies auth = loginAsAdmin();

        postJson(auth,
                "/api/operations/missao-eventos/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "ocorridoEm",
                          "granularity": "DAY",
                          "metric": { "operation": "COUNT", "alias": "events" },
                          "limit": 30,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("ocorridoEm"),
                jsonPath("$.data.granularity").value("DAY"),
                jsonPath("$.data.points[0].label").exists(),
                jsonPath("$.data.points[0].count").isNumber());
    }
}
