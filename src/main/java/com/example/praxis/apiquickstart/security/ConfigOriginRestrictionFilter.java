package com.example.praxis.apiquickstart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
/**
 * Aplica uma fronteira adicional de origem para a superficie {@code /api/praxis/config/**}.
 *
 * <p>No quickstart, os endpoints do {@code praxis-config-starter} podem estar publicamente
 * acessiveis na security chain para viabilizar integracoes de UI, AI e automacao. Ainda assim, o
 * host de referencia impoe uma defesa extra baseada em {@code Origin}/{@code Referer} para evitar
 * uso indevido fora das origens permitidas.</p>
 *
 * <p>Esse filtro e importante didaticamente porque mostra uma distincao frequente na plataforma:
 * "permitAll" em Spring Security nao significa "sem governanca operacional".</p>
 */
public class ConfigOriginRestrictionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ConfigOriginRestrictionFilter.class);

    private final boolean enabled;
    private final Set<String> allowedOrigins;

    public ConfigOriginRestrictionFilter(
            @Value("${app.security.config-origin-restriction.enabled:true}") boolean enabled,
            @Value("${app.security.config-origin-restriction.allowed-origins:https://praxisui-dev.web.app}") String allowedOrigins
    ) {
        this.enabled = enabled;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeOrigin)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/praxis/config/");
    }

    /** Resolve a origem efetiva mesmo atras de proxies ou clients sem header {@code Origin}. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String effectiveOrigin = resolveEffectiveOrigin(request);
        if (StringUtils.hasText(effectiveOrigin) && allowedOrigins.contains(effectiveOrigin)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[CONFIG_ORIGIN] Blocked request origin={} referer={} forwardedProto={} forwardedHost={} host={} effectiveOrigin={} method={} path={}",
                request.getHeader("Origin"),
                request.getHeader("Referer"),
                request.getHeader("X-Forwarded-Proto"),
                request.getHeader("X-Forwarded-Host"),
                request.getHeader("Host"),
                effectiveOrigin,
                request.getMethod(),
                request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":\"failure\",\"message\":\"Config origin not allowed\"}");
    }

    /** Tenta reconstruir a origem observando {@code Origin}, {@code Referer} e headers forwarded. */
    private String resolveEffectiveOrigin(HttpServletRequest request) {
        String origin = parseOriginHeader(request.getHeader("Origin"));
        if (StringUtils.hasText(origin)) {
            return origin;
        }

        String refererOrigin = parseUrlOrigin(request.getHeader("Referer"));
        if (StringUtils.hasText(refererOrigin)) {
            return refererOrigin;
        }

        String forwardedOrigin = buildOrigin(
                firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme()),
                firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"))
        );
        if (StringUtils.hasText(forwardedOrigin)) {
            return forwardedOrigin;
        }

        return buildOrigin(request.getScheme(), request.getHeader("Host"));
    }

    private String parseOriginHeader(String raw) {
        if (!StringUtils.hasText(raw) || "null".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return normalizeOrigin(raw);
    }

    private String parseUrlOrigin(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                return null;
            }
            return normalizeOrigin(uri.getScheme() + "://" + uri.getAuthority());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildOrigin(String scheme, String host) {
        if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
            return null;
        }
        return normalizeOrigin(scheme.trim() + "://" + host.trim());
    }

    private String normalizeOrigin(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                return null;
            }
            String normalized = uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase();
            if (uri.getPort() >= 0) {
                normalized += ":" + uri.getPort();
            }
            return normalized;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
