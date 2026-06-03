package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigReadOpenStatsPolicyTest.DummyStatsController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        PublicApiRateLimitFilter.class
        ,
        SecurityConfigReadOpenStatsPolicyTest.DummyStatsController.class,
        SecurityConfigReadOpenStatsPolicyTest.DummySchemasController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=true",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false"
})
class SecurityConfigReadOpenStatsPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldAllowStatsPostWithoutAuthenticationWhenReadOpenIsEnabled() throws Exception {
        mockMvc.perform(post("/api/test/demo/stats/group-by")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter":{},"field":"status","metric":{"operation":"COUNT"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void shouldAllowCollectionExportPostWithoutAuthenticationWhenReadOpenIsEnabled() throws Exception {
        mockMvc.perform(post("/api/test/demo/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"format":"csv","scope":"currentPage"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("export"));
    }

    @Test
    void shouldAllowOptionSourcesReadEndpointsWithoutAuthenticationWhenReadOpenIsEnabled() throws Exception {
        mockMvc.perform(post("/api/test/demo/option-sources/status/options/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"search":"ops"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("option-filter"));

        mockMvc.perform(get("/api/test/demo/option-sources/status/options/by-ids")
                        .param("ids", "ops", "exec"))
                .andExpect(status().isOk())
                .andExpect(content().string("option-by-ids"));
    }

    @Test
    void shouldAllowSchemasEndpointsWithoutAuthenticationWhenAggregatorIsEnabled() throws Exception {
        mockMvc.perform(get("/schemas/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string("catalog"));

        mockMvc.perform(post("/schemas/filtered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("filtered"));
    }

    @RestController
    static class DummyStatsController {

        @PostMapping("/api/test/demo/stats/group-by")
        String groupBy(@RequestBody String body) {
            return "ok";
        }

        @PostMapping("/api/test/demo/export")
        String export(@RequestBody String body) {
            return "export";
        }

        @PostMapping("/api/test/demo/option-sources/status/options/filter")
        String optionSourceFilter(@RequestBody String body) {
            return "option-filter";
        }

        @GetMapping("/api/test/demo/option-sources/status/options/by-ids")
        String optionSourceByIds() {
            return "option-by-ids";
        }
    }

    @RestController
    static class DummySchemasController {

        @GetMapping("/schemas/catalog")
        String catalog() {
            return "catalog";
        }

        @PostMapping("/schemas/filtered")
        String filtered(@RequestBody String body) {
            return "filtered";
        }
    }
}
