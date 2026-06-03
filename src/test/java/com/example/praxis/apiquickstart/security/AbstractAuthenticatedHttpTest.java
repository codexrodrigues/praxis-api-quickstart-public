package com.example.praxis.apiquickstart.security;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {
        "app.rate-limit.enabled=false",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=false",
        "app.security.write-disabled=false",
        "app.security.csrf.disable=false",
        "app.session.cookie-name=SESSION",
        "app.session.secure=false",
        "app.session.samesite=Lax",
        "praxis.stats.enabled=true",
        "praxis.ai.provider=mock",
        "spring.ai.embedding.provider=mock",
        "spring.ai.openai.api-key=dummy",
        "praxis.ai.rag.vector-store.enabled=false",
        "praxis.domain-catalog.rag-publication.enabled=false",
        "praxis.ai.registry.bootstrap.enabled=false",
        "praxis.ai.registry.health.enabled=false",
        "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false"
})
public abstract class AbstractAuthenticatedHttpTest {

    @Autowired
    protected MockMvc mockMvc;

    protected AuthCookies loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"changeMe!"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertNotNull(sessionCookie);

        MvcResult sessionResult = mockMvc.perform(get("/auth/session").cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie csrfCookie = sessionResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        return new AuthCookies(sessionCookie, csrfCookie);
    }

    protected AuthCookies postJson(
            AuthCookies auth,
            String path,
            String payload,
            ResultMatcher... expectations
    ) throws Exception {
        ResultActions action = mockMvc.perform(post(path)
                        .cookie(auth.session(), auth.csrf())
                        .header("X-XSRF-TOKEN", auth.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        for (ResultMatcher expectation : expectations) {
            action.andExpect(expectation);
        }

        MvcResult result = action.andReturn();
        Cookie sessionCookie = resolveLatestCookie(result, "SESSION");
        Cookie csrfCookie = resolveLatestCookie(result, "XSRF-TOKEN");

        return new AuthCookies(
                sessionCookie != null ? sessionCookie : auth.session(),
                csrfCookie != null ? csrfCookie : auth.csrf()
        );
    }

    protected Cookie resolveLatestCookie(MvcResult result, String name) {
        Cookie fallback = null;
        for (Cookie cookie : result.getResponse().getCookies()) {
            if (!name.equals(cookie.getName())) {
                continue;
            }
            fallback = cookie;
            if (cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie;
            }
        }
        return fallback;
    }

    protected record AuthCookies(Cookie session, Cookie csrf) {
    }
}
