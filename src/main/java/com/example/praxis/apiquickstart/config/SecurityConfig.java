package com.example.praxis.apiquickstart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.praxis.apiquickstart.security.CookieJwtAuthenticationFilter;
import com.example.praxis.apiquickstart.security.ConfigOriginRestrictionFilter;
import com.example.praxis.apiquickstart.security.PublicApiRateLimitFilter;
import com.example.praxis.apiquickstart.security.SpaCsrfTokenRequestHandler;
import org.praxisplatform.uischema.constants.ApiPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define a politica de exposicao HTTP do quickstart.
 *
 * <p>Esta configuracao e especialmente importante porque o projeto hospeda, no mesmo app:</p>
 *
 * <ul>
 *   <li>rotas publicas de docs e schemas do {@code praxis-metadata-starter};</li>
 *   <li>rotas de configuracao e AI de {@code /api/praxis/config/**} vindas do
 *   {@code praxis-config-starter};</li>
 *   <li>recursos de negocio do dominio de exemplo do quickstart.</li>
 * </ul>
 *
 * <p>O papel desta classe nao e apenas "ligar Spring Security". Ela documenta quais superficies da
 * plataforma sao publicas, quais dependem de sessao, quais filtros extras incidem sobre config e
 * como as flags de demo afetam a exposure policy do host de referencia.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.csrf.disable:false}")
    private boolean csrfDisable;

    @Value("${app.security.read-open:false}")
    private boolean readOpen;

    @Value("${app.security.read-open.whitelist:}")
    private String readOpenWhitelist;

    @Value("${app.security.write-disabled:false}")
    private boolean writeDisabled;

    @Value("${app.security.schemas-aggregator.enabled:true}")
    private boolean schemasAggregatorEnabled;

    @Value("${app.security.demo-allow-bulk-actions:false}")
    private boolean demoAllowBulkActions;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CookieJwtAuthenticationFilter cookieFilter,
                                            ConfigOriginRestrictionFilter configOriginRestrictionFilter,
                                            PublicApiRateLimitFilter publicApiRateLimitFilter) throws Exception {
        final String[] readOpenWhitelistPatterns = (readOpenWhitelist == null || readOpenWhitelist.trim().isEmpty())
                ? new String[0]
                : java.util.Arrays.stream(readOpenWhitelist.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // Startup logs: summarize security flags and open endpoints
        log.info("[SECURITY] csrfDisable={}, readOpen={}, writeDisabled={}, schemasAggregatorEnabled={}", csrfDisable, readOpen, writeDisabled, schemasAggregatorEnabled);
        log.info("[SECURITY] read-open whitelist (raw)='{}' (resolved {} pattern(s))", readOpenWhitelist, readOpenWhitelistPatterns.length);
        log.info("[SECURITY] Base public endpoints (permitAll): /auth/login, /auth/logout, /auth/session, /swagger-ui.html, /swagger-ui/index.html, /swagger-ui/**, /v3/api-docs, /v3/api-docs/**, /v3/api-docs.yaml, /actuator/health, /actuator/health/**, /actuator/info, /, /index.html, /favicon.ico, /assets/**, {}, {}/, {}",
                ApiPaths.Framework.COCKPIT,
                ApiPaths.Framework.COCKPIT,
                ApiPaths.Framework.COCKPIT_PATTERN);
        log.info("[SECURITY] /actuator/env requires authentication (and is not exposed by default).");
        if (readOpen) {
            log.info("[SECURITY] Read-Open=true -> GET/HEAD allowed for /api/**");
            log.info("[SECURITY] Read-Open=true -> Extra GET allowed: {}, {}, {}, {}, {}",
                    "/api/*/*/schemas/**",
                    "/api/*/*/schema/**",
                    "/api/*/*/filters/**",
                    "/api/*/*/options/**",
                    "/api/*/*/option-sources/**",
                    "/api/*/*/filtered");
            log.info("[SECURITY] Read-Open=true -> Extra POST allowed: {}, {}, {}, {}, {}, {}, {}",
                    "/api/*/*/filters/**",
                    "/api/*/*/filter",
                    "/api/*/*/filter/cursor",
                    "/api/*/*/locate",
                    "/api/*/*/filtered",
                    "/api/*/*/export",
                    "/api/*/*/options/**",
                    "/api/*/*/option-sources/**");
            log.info("[SECURITY] Read-Open=true -> Stats POST allowed: {}", "/api/*/*/stats/**");
            log.info("[SECURITY] Read-Open=true -> Runtime context switch allowed: PUT /api/praxis/runtime/context");
        }
        if (demoAllowBulkActions) {
            log.info("[SECURITY] DemoAllowBulkActions=true -> Allowing POST /api/*/*/actions/**");
        }
        if (readOpenWhitelistPatterns.length > 0) {
            log.info("[SECURITY] Whitelist GET/HEAD patterns (prod): {}", java.util.Arrays.toString(readOpenWhitelistPatterns));
        } else if (!readOpen) {
            log.info("[SECURITY] No public API read endpoints configured (read-open=false, empty whitelist). /api/** requires authentication.");
        }
        if (writeDisabled) {
            log.info("[SECURITY] Write-Disabled=true -> Denying DELETE, PATCH, PUT, POST for /api/** (except explicit allowances above)");
        }
        log.info("[SECURITY] OPTIONS /** is permitted for CORS preflight");
        if (schemasAggregatorEnabled) {
            log.info("[SECURITY] Schemas endpoints are public: GET /schemas/** and POST /schemas/filtered");
        } else {
            log.info("[SECURITY] Schemas endpoints are NOT public");
        }
        if (csrfDisable) {
            http.csrf(csrf -> csrf.disable());
            log.info("[SECURITY] CSRF disabled");
        } else {
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/auth/login", "/auth/logout", "/api/praxis/config/**")
            );
            log.info("[SECURITY] CSRF enabled with SPA cookie/header handler; ignoring tokens for: /auth/login, /auth/logout, /api/praxis/config/**");
        }
        http.cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .xssProtection(x -> {})
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentTypeOptions(c -> {})
            )
            .authorizeHttpRequests(auth -> {
                auth
                // Auth endpoints: permitir login/logout e checagem de sessão (controlador valida se autenticado)
                .requestMatchers("/auth/login", "/auth/logout", "/auth/session").permitAll()
                // Swagger UI e OpenAPI públicas
                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/index.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                ).permitAll()
                // Health público para health checks (Render)
                .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/info"
                ).permitAll()
                // /actuator/env nunca e publico (mesmo com read-open=true).
                .requestMatchers("/actuator/env", "/actuator/env/**").authenticated()
                // Home pública e assets estáticos
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/favicon.ico",
                        "/assets/**",
                        ApiPaths.Framework.COCKPIT,
                        ApiPaths.Framework.COCKPIT + "/",
                        ApiPaths.Framework.COCKPIT_PATTERN
                ).permitAll()
                // Endpoints do config-store/RAG (ingestão de registry/catalog)
                .requestMatchers("/api/praxis/config/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/praxis/config/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/praxis/config/**").permitAll()
                ;

                if (schemasAggregatorEnabled) {
                    auth.requestMatchers(HttpMethod.GET, "/schemas/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/schemas/filtered").permitAll();
                }

                if (readOpen) {
                    // Abrir leitura pública (GET/HEAD) para toda a API
                    auth
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/**").permitAll()
                        // Abrir endpoints de metadados e filtros comuns, inclusive se expostos via POST
                        .requestMatchers(HttpMethod.GET,
                                "/api/*/*/schemas/**",
                                "/api/*/*/schema/**",
                                "/api/*/*/filters/**",
                                "/api/*/*/options/**",
                                "/api/*/*/option-sources/**",
                                "/api/*/*/filtered"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/*/*/filters/**",
                                "/api/*/*/filter",
                                "/api/*/*/filter/cursor",
                                "/api/*/*/locate",
                                "/api/*/*/filtered",
                                "/api/*/*/export",
                                "/api/*/*/options/**",
                                "/api/*/*/option-sources/**",
                                "/api/*/*/stats/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/praxis/runtime/context").permitAll();

                    if (demoAllowBulkActions) {
                        auth.requestMatchers(HttpMethod.POST, "/api/*/*/actions/**").permitAll();
                    }
                }

                if (readOpenWhitelistPatterns.length > 0) {
                    // Lista branca de leitura pública (GET/HEAD) por paths específicos (produção)
                    auth.requestMatchers(HttpMethod.GET, readOpenWhitelistPatterns).permitAll();
                    auth.requestMatchers(HttpMethod.HEAD, readOpenWhitelistPatterns).permitAll();
                }

                if (writeDisabled) {
                    // Bloquear escrita na API: POST/PUT/PATCH/DELETE
                    // Observação: regras específicas de POST liberadas acima (filters/options/filtered) prevalecem
                    auth
                        .requestMatchers(HttpMethod.DELETE, "/api/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/**").denyAll();
                }

                // Demais endpoints exigem autenticação, mesmo com read-open=true.
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(configOriginRestrictionFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(publicApiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new com.example.praxis.apiquickstart.security.CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    /**
     * Permite chaves de configuracao com barras codificadas em rotas do config-starter.
     *
     * <p>Isso e necessario porque {@code componentId} e outras chaves publicas da superficie
     * {@code /api/praxis/config/**} podem carregar {@code /} como parte do identificador
     * canonico.</p>
     */
    @Bean
    public HttpFirewall allowUrlEncodedSlashFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedDoubleSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowSemicolon(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return web -> web.httpFirewall(firewall);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        if ("*".equals(allowedOrigins.trim())) {
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
            log.info("[CORS] AllowedOriginPattern='*' (credentials=false)");
        } else {
            for (String origin : allowedOrigins.split(",")) {
                String o = origin.trim();
                if (!o.isEmpty()) config.addAllowedOrigin(o);
            }
            config.setAllowCredentials(true);
            log.info("[CORS] AllowedOrigins={} (credentials=true)", config.getAllowedOrigins());
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.setMaxAge(3600L);

        log.info("[CORS] AllowedMethods={} AllowedHeaders='*' MaxAge={}s",
                config.getAllowedMethods(), config.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
