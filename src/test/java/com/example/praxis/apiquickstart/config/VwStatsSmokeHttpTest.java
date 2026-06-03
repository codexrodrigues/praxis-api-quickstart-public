package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.security.AbstractAuthenticatedHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "PRAXIS_EXTERNAL_SMOKE_TESTS", matches = "true")
class VwStatsSmokeHttpTest extends AbstractAuthenticatedHttpTest {

    @Test
    void shouldAcceptAngularChartsCanonicalStatsRequests() throws Exception {
        AuthCookies auth = loginAsAdmin();

        auth = postJson(auth,
                "/api/human-resources/vw-perfil-heroi/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "universo",
                          "metric": { "operation": "COUNT" },
                          "limit": 10,
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("universo"),
                jsonPath("$.data.buckets[0].label").exists(),
                jsonPath("$.data.buckets.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(10)));

        auth = postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "ocorridoEm",
                          "granularity": "MONTH",
                          "metric": { "operation": "COUNT" },
                          "fillGaps": false
                        }
                        """,
                jsonPath("$.data.field").value("ocorridoEm"),
                jsonPath("$.data.granularity").value("MONTH"),
                jsonPath("$.data.points[0].label").exists(),
                jsonPath("$.data.points[0].count").isNumber());

        postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "totalPago",
                          "mode": "HISTOGRAM",
                          "metric": { "operation": "COUNT" },
                          "bucketSize": 5000,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("totalPago"),
                jsonPath("$.data.mode").value("HISTOGRAM"),
                jsonPath("$.data.buckets[0].from").exists(),
                jsonPath("$.data.buckets[0].to").exists());
    }

    @Test
    void shouldServeSmokeStatsForVwPerfilHeroi() throws Exception {
        AuthCookies auth = loginAsAdmin();

        auth = postJson(auth,
                "/api/human-resources/vw-perfil-heroi/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "universo",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("universo"),
                jsonPath("$.data.buckets[0].label").exists(),
                jsonPath("$.data.buckets[0].count").isNumber());

        auth = postJson(auth,
                "/api/human-resources/vw-perfil-heroi/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "departamento",
                          "mode": "TERMS",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("departamento"),
                jsonPath("$.data.mode").value("TERMS"),
                jsonPath("$.data.buckets[0].label").exists());

        postJson(auth,
                "/api/human-resources/vw-perfil-heroi/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "scoreMedio",
                          "mode": "HISTOGRAM",
                          "metric": { "operation": "COUNT" },
                          "bucketSize": 10,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("scoreMedio"),
                jsonPath("$.data.mode").value("HISTOGRAM"),
                jsonPath("$.data.buckets[0].from").exists(),
                jsonPath("$.data.buckets[0].to").exists());
    }

    @Test
    void shouldServeSmokeStatsForVwIndicadoresIncidentes() throws Exception {
        AuthCookies auth = loginAsAdmin();

        auth = postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "severidade",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("severidade"),
                jsonPath("$.data.buckets[0].label").exists(),
                jsonPath("$.data.buckets[0].count").isNumber());

        auth = postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "ocorridoEm",
                          "granularity": "MONTH",
                          "metric": { "operation": "COUNT" },
                          "fillGaps": false
                        }
                        """,
                jsonPath("$.data.field").value("ocorridoEm"),
                jsonPath("$.data.granularity").value("MONTH"),
                jsonPath("$.data.points[0].label").exists(),
                jsonPath("$.data.points[0].count").isNumber());

        auth = postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "severidade",
                          "mode": "TERMS",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("severidade"),
                jsonPath("$.data.mode").value("TERMS"),
                jsonPath("$.data.buckets[0].key").exists());

        postJson(auth,
                "/api/risk-intelligence/vw-indicadores-incidentes/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "totalPago",
                          "mode": "HISTOGRAM",
                          "metric": { "operation": "COUNT" },
                          "bucketSize": 5000,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("totalPago"),
                jsonPath("$.data.mode").value("HISTOGRAM"),
                jsonPath("$.data.buckets[0].from").exists(),
                jsonPath("$.data.buckets[0].to").exists());
    }

    @Test
    void shouldServeSmokeStatsForVwAnalyticsFolhaPagamento() throws Exception {
        AuthCookies auth = loginAsAdmin();

        auth = postJson(auth,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/group-by",
                """
                        {
                          "filter": {},
                          "field": "payrollProfile",
                          "metric": { "operation": "COUNT" },
                          "orderBy": "VALUE_DESC"
                        }
                        """,
                jsonPath("$.data.field").value("payrollProfile"),
                jsonPath("$.data.buckets[0].label").exists(),
                jsonPath("$.data.buckets[0].count").isNumber());

        auth = postJson(auth,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "competencia",
                          "granularity": "MONTH",
                          "metric": { "operation": "SUM", "field": "salarioLiquido" },
                          "fillGaps": false
                        }
                        """,
                jsonPath("$.data.field").value("competencia"),
                jsonPath("$.data.granularity").value("MONTH"),
                jsonPath("$.data.points[0].label").exists(),
                jsonPath("$.data.points[0].value").isNumber());

        auth = postJson(auth,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/timeseries",
                """
                        {
                          "filter": {},
                          "field": "competencia",
                          "granularity": "MONTH",
                          "metrics": [
                            { "operation": "SUM", "field": "salarioLiquido", "alias": "massaLiquida" },
                            { "operation": "AVG", "field": "pctDesconto", "alias": "mediaPctDesconto" }
                          ],
                          "fillGaps": false
                        }
                        """,
                jsonPath("$.data.field").value("competencia"),
                jsonPath("$.data.granularity").value("MONTH"),
                jsonPath("$.data.metrics.length()").value(2),
                jsonPath("$.data.points[0].label").exists(),
                jsonPath("$.data.points[0].value").isNumber(),
                jsonPath("$.data.points[0].values.massaLiquida").isNumber(),
                jsonPath("$.data.points[0].values.mediaPctDesconto").isNumber());

        postJson(auth,
                "/api/human-resources/vw-analytics-folha-pagamento/stats/distribution",
                """
                        {
                          "filter": {},
                          "field": "salarioLiquido",
                          "mode": "HISTOGRAM",
                          "metric": { "operation": "COUNT" },
                          "bucketSize": 5000,
                          "orderBy": "KEY_ASC"
                        }
                        """,
                jsonPath("$.data.field").value("salarioLiquido"),
                jsonPath("$.data.mode").value("HISTOGRAM"),
                jsonPath("$.data.buckets[0].from").exists(),
                jsonPath("$.data.buckets[0].to").exists());
    }

    @Test
    void shouldServeOptionSourcesForAnalyticsViews() throws Exception {
        AuthCookies auth = loginAsAdmin();

        mockMvc.perform(post("/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universoContexto": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].label").exists())
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));

        mockMvc.perform(post("/api/human-resources/vw-analytics-folha-pagamento/option-sources/faixaPctDesconto/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payrollProfile": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].label").exists())
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));

        mockMvc.perform(post("/api/human-resources/vw-perfil-heroi/option-sources/universo/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exposicaoPublica": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].label").exists())
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));

        mockMvc.perform(post("/api/risk-intelligence/vw-indicadores-incidentes/option-sources/severidade/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ocorridoEmLastDays": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].label").exists())
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));

        mockMvc.perform(get("/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/by-ids")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("ids", "OPERATIONS", "EXEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void shouldServeOptionSourcesWithAngularLikeQueryPatterns() throws Exception {
        AuthCookies auth = loginAsAdmin();

        mockMvc.perform(post("/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "5")
                        .param("search", "OPER")
                        .param("includeIds", "EXEC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.hasItems("EXEC", "OPERATIONS")));

        mockMvc.perform(post("/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/filter")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universoContexto": "Marvel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/human-resources/vw-analytics-folha-pagamento/option-sources/payrollProfile/options/by-ids")
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .param("ids", "OPERATIONS", "EXEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("OPERATIONS"))
                .andExpect(jsonPath("$[1].id").value("EXEC"));
    }

}
