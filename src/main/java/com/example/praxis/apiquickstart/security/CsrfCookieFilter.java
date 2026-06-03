package com.example.praxis.apiquickstart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Garante a materializacao do token CSRF em cookie quando a policy do host esta ativa.
 *
 * <p>O filtro nao decide autorizacao nem valida o token. Ele apenas força a resolucao do
 * {@link CsrfToken} para que o repositorio baseado em cookie tenha um valor disponivel para SPAs e
 * clientes browser-based que consomem o quickstart.</p>
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            // Access the token to ensure it's generated and saved to the cookie repository
            csrf.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
