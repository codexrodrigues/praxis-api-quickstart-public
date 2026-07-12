package com.example.praxis.apiquickstart.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigAiPatchPolicyTest.DummyAiPatchController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        CookieJwtAuthenticationFilter.class,
        ConfigOriginRestrictionFilter.class,
        TrustedProxyPolicy.class,
        PublicApiRateLimitFilter.class,
        SecurityConfigAiPatchPolicyTest.DummyAiPatchController.class
})
@TestPropertySource(properties = {
        "app.security.csrf.disable=true",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=false",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.rate-limit.enabled=false"
})
class SecurityConfigAiPatchPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldAllowAiPatchPathAtSecurityPolicyLevelUsingDummyController() throws Exception {
        mockMvc.perform(post("/api/praxis/config/ai/patch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "componentId": "praxis-table",
                                  "componentType": "table"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("patched"));
    }

    @RestController
    static class DummyAiPatchController {

        @PostMapping("/api/praxis/config/ai/patch")
        String patch(@RequestBody String body) {
            return "patched";
        }
    }
}
