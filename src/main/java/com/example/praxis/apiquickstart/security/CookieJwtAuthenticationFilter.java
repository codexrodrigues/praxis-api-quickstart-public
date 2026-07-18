package com.example.praxis.apiquickstart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converte a sessao do quickstart em autenticacao Spring Security.
 *
 * <p>Este filtro e a ponte entre o login simplificado do host e o restante da cadeia de seguranca:
 * ele aceita o cookie HttpOnly do browser ou um Bearer JWT emitido pelo proprio host para chamadas
 * internas governadas, valida o token localmente e popula o contexto usado pelos endpoints
 * protegidos.</p>
 */
@Component
public class CookieJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final String sessionCookieName;

    public CookieJwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                         @Value("${app.session.cookie-name}") String sessionCookieName) {
        this.jwtTokenService = jwtTokenService;
        this.sessionCookieName = sessionCookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extractToken(request);
            if (token != null && !token.isBlank()) {
                JwtTokenService.JwtValidationResult result = jwtTokenService.validate(token);
                if (result.valid()) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (result.role() != null && !result.role().isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + result.role()));
                    }
                    for (String authority : result.authorities()) {
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                    var auth = new UsernamePasswordAuthenticationToken(
                            result.subject(), null, authorities
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearer = authorization.substring(7).trim();
            if (!bearer.isEmpty()) {
                return bearer;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (sessionCookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
