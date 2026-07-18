package com.example.praxis.apiquickstart.security;

import java.util.Optional;
import org.praxisplatform.config.service.GovernedPlatformRequest;
import org.praxisplatform.config.service.GovernedPlatformRequestAuthorizationProvider;
import org.springframework.stereotype.Component;

/**
 * Adaptador de referencia entre o principal resolvido pelo Config Starter e a seguranca do host.
 *
 * <p>O starter publica apenas o SPI. Este quickstart conhece seu catalogo demonstrativo de
 * principals e pode emitir um JWT de delegacao curto. Hosts corporativos devem substituir esta
 * estrategia por token exchange, workload identity ou mecanismo equivalente do seu IAM.</p>
 */
@Component
public class QuickstartGovernedPlatformRequestAuthorizationProvider
        implements GovernedPlatformRequestAuthorizationProvider {

    private final JwtTokenService jwtTokenService;
    private final QuickstartPrincipalAuthorityCatalog principalCatalog;

    public QuickstartGovernedPlatformRequestAuthorizationProvider(
            JwtTokenService jwtTokenService,
            QuickstartPrincipalAuthorityCatalog principalCatalog) {
        this.jwtTokenService = jwtTokenService;
        this.principalCatalog = principalCatalog;
    }

    @Override
    public Optional<String> authorizationHeader(GovernedPlatformRequest request) {
        if (request == null || !request.isSameOrigin() || !matchesCanonicalSurface(request)) {
            return Optional.empty();
        }

        Optional<QuickstartPrincipalAuthorityCatalog.PrincipalGrant> resolved =
                principalCatalog.resolve(request.userId());
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        QuickstartPrincipalAuthorityCatalog.PrincipalGrant principal = resolved.orElseThrow();

        String token = jwtTokenService.generateDelegation(
                principal.subject(),
                principal.role(),
                principal.authorities());
        return Optional.of("Bearer " + token);
    }

    private boolean matchesCanonicalSurface(GovernedPlatformRequest request) {
        String path = request.targetUri().getPath();
        return switch (request.surface()) {
            case SCHEMA_FILTERED -> "/schemas/filtered".equals(path);
            case RESOURCE_CAPABILITIES -> path != null
                    && path.startsWith("/api/")
                    && path.endsWith("/capabilities");
            case RESOURCE_SURFACE_CATALOG -> "/schemas/surfaces".equals(path);
        };
    }
}
