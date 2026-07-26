package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.TrustedProxyPolicy;
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
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HrAnalyticsReadOpenSecurityPolicyTest.ReadOpenAnalyticsEndpoints.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        HrAnalyticsReadOpenSecurityPolicyTest.ReadOpenAnalyticsEndpoints.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=true",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false",
        "app.session.cookie-name=SESSION"
})
class HrAnalyticsReadOpenSecurityPolicyTest {
    private static final String PAYROLL = "/api/human-resources/vw-analytics-folha-pagamento";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldExposeDemoPayrollQueriesWithoutLoginWhenReadOpenIsEnabled() throws Exception {
        mockMvc.perform(get(PAYROLL + "/all"))
                .andExpect(status().isOk())
                .andExpect(content().string("rows"));

        mockMvc.perform(post(PAYROLL + "/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("filter"));

        mockMvc.perform(post(PAYROLL + "/stats/group-by")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("group-by"));

        mockMvc.perform(post(PAYROLL + "/stats/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("comparison"));
    }

    @RestController
    static class ReadOpenAnalyticsEndpoints {
        @GetMapping(PAYROLL + "/all")
        String rows() {
            return "rows";
        }

        @PostMapping(PAYROLL + "/filter")
        String filter() {
            return "filter";
        }

        @PostMapping(PAYROLL + "/stats/group-by")
        String groupBy() {
            return "group-by";
        }

        @PostMapping(PAYROLL + "/stats/comparison")
        String comparison() {
            return "comparison";
        }
    }
}
