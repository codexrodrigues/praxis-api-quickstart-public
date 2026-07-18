package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.TrustedProxyPolicy;
import jakarta.servlet.http.Cookie;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HrAnalyticsSecurityPolicyTest.HrAnalyticsEndpoints.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        HrAnalyticsSecurityPolicyTest.HrAnalyticsEndpoints.class
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
class HrAnalyticsSecurityPolicyTest {
    private static final String ANALYTICS = "/api/human-resources/vw-analytics-afastamentos";
    private static final String PAYROLL = "/api/human-resources/vw-analytics-folha-pagamento";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldNotLetReadOpenBypassNominalAnalyticsAuthority() throws Exception {
        mockMvc.perform(get(ANALYTICS + "/row-1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PAYROLL + "/101"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ANALYTICS + "/row-1").cookie(session("aggregate")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAggregateOnlyPrincipalToCompareButNotReadRows() throws Exception {
        mockMvc.perform(post(ANALYTICS + "/stats/comparison")
                        .cookie(session("aggregate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("comparison"));
        mockMvc.perform(post(PAYROLL + "/stats/comparison")
                        .cookie(session("aggregate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("payroll-comparison"));
    }

    @Test
    void shouldAllowAggregateAndNominalPrincipalsToDiscoverCollectionCapabilities() throws Exception {
        mockMvc.perform(get(ANALYTICS + "/capabilities"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ANALYTICS + "/capabilities").cookie(session("aggregate")))
                .andExpect(status().isOk())
                .andExpect(content().string("capabilities"));

        mockMvc.perform(get(ANALYTICS + "/capabilities").cookie(session("nominal")))
                .andExpect(status().isOk())
                .andExpect(content().string("capabilities"));
        mockMvc.perform(get(PAYROLL + "/capabilities").cookie(session("aggregate")))
                .andExpect(status().isOk())
                .andExpect(content().string("payroll-capabilities"));
    }

    @Test
    void shouldAuthenticateAHostDelegationBearerWithoutRequiringABrowserCookie() throws Exception {
        given(jwtTokenService.validate("delegated-aggregate")).willReturn(
                JwtTokenService.JwtValidationResult.valid(
                        "aggregate-only",
                        "USER",
                        List.of(HrAnalyticsAuthorities.AGGREGATE_READ)));

        mockMvc.perform(get(ANALYTICS + "/capabilities")
                        .header("Authorization", "Bearer delegated-aggregate"))
                .andExpect(status().isOk())
                .andExpect(content().string("capabilities"));
    }

    @Test
    void shouldAllowNominalPrincipalToReadRows() throws Exception {
        mockMvc.perform(get(ANALYTICS + "/row-1").cookie(session("nominal")))
                .andExpect(status().isOk())
                .andExpect(content().string("nominal"));
        mockMvc.perform(get(PAYROLL + "/101").cookie(session("nominal")))
                .andExpect(status().isOk())
                .andExpect(content().string("payroll-nominal"));
    }

    @Test
    void shouldRequireEmployee360AuthorityForHeroProfile() throws Exception {
        mockMvc.perform(get("/api/human-resources/funcionarios/7/hero-profile").cookie(session("aggregate")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/human-resources/funcionarios/7/hero-profile").cookie(session("employee-360")))
                .andExpect(status().isOk())
                .andExpect(content().string("hero"));
    }

    @Test
    void shouldApplyTheSameAuthorityPolicyToHeadRequests() throws Exception {
        mockMvc.perform(head(ANALYTICS + "/row-1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(head(ANALYTICS + "/row-1").cookie(session("aggregate")))
                .andExpect(status().isForbidden());

        mockMvc.perform(head(ANALYTICS + "/row-1").cookie(session("nominal")))
                .andExpect(status().isOk());
        mockMvc.perform(head(PAYROLL + "/101").cookie(session("aggregate")))
                .andExpect(status().isForbidden());
        mockMvc.perform(head(PAYROLL + "/101").cookie(session("nominal")))
                .andExpect(status().isOk());

        mockMvc.perform(head("/api/human-resources/funcionarios/7/hero-profile"))
                .andExpect(status().isForbidden());

        mockMvc.perform(head("/api/human-resources/funcionarios/7/hero-profile").cookie(session("employee-360")))
                .andExpect(status().isOk());
    }

    private Cookie session(String token) {
        List<String> authorities = switch (token) {
            case "aggregate" -> List.of(HrAnalyticsAuthorities.AGGREGATE_READ);
            case "nominal" -> List.of(HrAnalyticsAuthorities.NOMINAL_READ);
            case "employee-360" -> List.of(HrAnalyticsAuthorities.EMPLOYEE_360_READ);
            default -> List.of();
        };
        given(jwtTokenService.validate(token)).willReturn(
                JwtTokenService.JwtValidationResult.valid("test-user", "USER", authorities)
        );
        return new Cookie("SESSION", token);
    }

    @RestController
    static class HrAnalyticsEndpoints {
        @GetMapping(ANALYTICS + "/{id}")
        String analyticsRow() {
            return "nominal";
        }

        @GetMapping(ANALYTICS + "/capabilities")
        String capabilities() {
            return "capabilities";
        }

        @PostMapping(ANALYTICS + "/stats/comparison")
        String comparison() {
            return "comparison";
        }

        @GetMapping(PAYROLL + "/{id}")
        String payrollRow() {
            return "payroll-nominal";
        }

        @GetMapping(PAYROLL + "/capabilities")
        String payrollCapabilities() {
            return "payroll-capabilities";
        }

        @PostMapping(PAYROLL + "/stats/comparison")
        String payrollComparison() {
            return "payroll-comparison";
        }

        @GetMapping("/api/human-resources/funcionarios/{id}/hero-profile")
        String hero() {
            return "hero";
        }
    }
}
