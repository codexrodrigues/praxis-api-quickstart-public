package com.example.praxis.apiquickstart.security;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
/**
 * Aplica rate limit em superficies publicas do quickstart.
 *
 * <p>O objetivo aqui nao e implementar a solucao definitiva de producao, mas explicitar uma
 * politica minima de protecao para o host publico de referencia. O filtro cobre categorias
 * diferentes de trafego exposto pelo quickstart, como login, leitura publica, queries metadata-
 * driven, bulk actions e chamadas do config-starter.</p>
 *
 * <p>Ele tambem serve como exemplo de onde colocar governanca operacional do host sem contaminar a
 * semantica canonica dos starters.</p>
 */
public class PublicApiRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(PublicApiRateLimitFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RateLimiterService rateLimiterService;
    private final boolean enabled;
    private final int loginLimit;
    private final long loginWindowMs;
    private final int publicReadLimit;
    private final long publicReadWindowMs;
    private final int publicQueryLimit;
    private final long publicQueryWindowMs;
    private final int bulkActionLimit;
    private final long bulkActionWindowMs;
    private final int configLimit;
    private final long configWindowMs;

    public PublicApiRateLimitFilter(
            RateLimiterService rateLimiterService,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.login.limit:10}") int loginLimit,
            @Value("${app.rate-limit.login.window-ms:60000}") long loginWindowMs,
            @Value("${app.rate-limit.public-read.limit:120}") int publicReadLimit,
            @Value("${app.rate-limit.public-read.window-ms:60000}") long publicReadWindowMs,
            @Value("${app.rate-limit.public-query.limit:60}") int publicQueryLimit,
            @Value("${app.rate-limit.public-query.window-ms:60000}") long publicQueryWindowMs,
            @Value("${app.rate-limit.bulk-action.limit:5}") int bulkActionLimit,
            @Value("${app.rate-limit.bulk-action.window-ms:60000}") long bulkActionWindowMs,
            @Value("${app.rate-limit.config.limit:120}") int configLimit,
            @Value("${app.rate-limit.config.window-ms:60000}") long configWindowMs
    ) {
        this.rateLimiterService = rateLimiterService;
        this.enabled = enabled;
        this.loginLimit = loginLimit;
        this.loginWindowMs = loginWindowMs;
        this.publicReadLimit = publicReadLimit;
        this.publicReadWindowMs = publicReadWindowMs;
        this.publicQueryLimit = publicQueryLimit;
        this.publicQueryWindowMs = publicQueryWindowMs;
        this.bulkActionLimit = bulkActionLimit;
        this.bulkActionWindowMs = bulkActionWindowMs;
        this.configLimit = configLimit;
        this.configWindowMs = configWindowMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        String limitKey = rule.keyPrefix + ":" + clientKey;
        boolean allowed = rateLimiterService.allow(limitKey, rule.limit, rule.windowMs);
        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[RATE_LIMIT] Blocked request rule={} client={} method={} path={}",
                rule.keyPrefix, clientKey, request.getMethod(), request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(Math.max(1, rule.windowMs / 1000)));
        response.getWriter().write("{\"status\":\"failure\",\"message\":\"Too many requests\"}");
    }

    /** Escolhe a janela correta conforme a superficie publica atingida. */
    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && "/auth/login".equals(path)) {
            return new RateLimitRule("login", loginLimit, loginWindowMs);
        }
        if (HttpMethod.POST.matches(method) && PATH_MATCHER.match("/api/*/*/actions/**", path)) {
            return new RateLimitRule("bulk-action", bulkActionLimit, bulkActionWindowMs);
        }
        if (PATH_MATCHER.match("/api/praxis/config/**", path)) {
            return new RateLimitRule("config", configLimit, configWindowMs);
        }
        if ("/schemas/filtered".equals(path)) {
            return new RateLimitRule("public-query", publicQueryLimit, publicQueryWindowMs);
        }
        if ((HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)) && PATH_MATCHER.match("/api/**", path)) {
            return new RateLimitRule("public-read", publicReadLimit, publicReadWindowMs);
        }
        if (HttpMethod.POST.matches(method) && isPublicQueryPath(path)) {
            return new RateLimitRule("public-query", publicQueryLimit, publicQueryWindowMs);
        }
        return null;
    }

    private boolean isPublicQueryPath(String path) {
        return PATH_MATCHER.match("/api/*/*/filters/**", path)
                || PATH_MATCHER.match("/api/*/*/filter", path)
                || PATH_MATCHER.match("/api/*/*/filter/cursor", path)
                || PATH_MATCHER.match("/api/*/*/locate", path)
                || PATH_MATCHER.match("/api/*/*/filtered", path)
                || PATH_MATCHER.match("/api/*/*/options/**", path);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private record RateLimitRule(String keyPrefix, int limit, long windowMs) {
    }
}
