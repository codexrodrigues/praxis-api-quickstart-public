package com.example.praxis.apiquickstart.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.Locale;

/**
 * Mantem o firewall estrito para o host inteiro e abre slash codificado apenas para config.
 *
 * <p>A excecao existe porque algumas rotas do {@code praxis-config-starter} recebem
 * {@code componentId} por {@code @PathVariable}, e esse identificador pode conter refs canonicas
 * com {@code /}. Nenhuma outra rota do quickstart ganha permissao de path encoding por tabela.</p>
 */
public class ConfigScopedEncodedSlashHttpFirewall implements HttpFirewall {
    private static final String CONFIG_PREFIX = "/api/praxis/config/";

    private final StrictHttpFirewall strictFirewall;
    private final StrictHttpFirewall configFirewall;

    public ConfigScopedEncodedSlashHttpFirewall() {
        this.strictFirewall = new StrictHttpFirewall();
        this.configFirewall = new StrictHttpFirewall();
        this.configFirewall.setAllowUrlEncodedSlash(true);
        this.configFirewall.setAllowUrlEncodedPercent(true);
    }

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
        if (isConfigRequest(request)) {
            rejectAmbiguousConfigEncoding(request);
            return configFirewall.getFirewalledRequest(request);
        }
        return strictFirewall.getFirewalledRequest(request);
    }

    @Override
    public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
        return strictFirewall.getFirewalledResponse(response);
    }

    private boolean isConfigRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.toLowerCase(Locale.ROOT).startsWith(CONFIG_PREFIX);
    }

    private void rejectAmbiguousConfigEncoding(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return;
        }
        String normalizedUri = uri.toLowerCase(Locale.ROOT);
        if (normalizedUri.contains("%25")) {
            throw new RequestRejectedException("The request was rejected because the config URL contained an encoded percent");
        }
        if (normalizedUri.contains("%2f%2f")) {
            throw new RequestRejectedException("The request was rejected because the config URL contained an encoded double slash");
        }
    }
}
