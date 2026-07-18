package com.example.praxis.apiquickstart.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.service.GovernedPlatformRequest;

class QuickstartGovernedPlatformRequestAuthorizationProviderTest {

    private final JwtTokenService tokenService = new JwtTokenService(
            "praxis-governed-request-provider-test-secret",
            "60");
    private final QuickstartGovernedPlatformRequestAuthorizationProvider provider =
            new QuickstartGovernedPlatformRequestAuthorizationProvider(
                    tokenService,
                    new QuickstartPrincipalAuthorityCatalog("admin", "demo", false));

    @Test
    void delegatesTheCanonicalAdminAuthoritiesToSameOriginGrounding() {
        String authorization = provider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/human-resources/vw-analytics-afastamentos/capabilities",
                "admin")).orElseThrow();

        JwtTokenService.JwtValidationResult result = tokenService.validate(bearerToken(authorization));

        assertThat(result.valid()).isTrue();
        assertThat(result.subject()).isEqualTo("admin");
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.authorities()).containsExactlyInAnyOrder(
                HrAnalyticsAuthorities.AGGREGATE_READ,
                HrAnalyticsAuthorities.NOMINAL_READ,
                HrAnalyticsAuthorities.EMPLOYEE_360_READ);
    }

    @Test
    void delegatesOnlyAggregateReadForTheAggregateOnlyPrincipal() {
        String authorization = provider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/human-resources/vw-analytics-afastamentos/capabilities",
                QuickstartPrincipalAuthorityCatalog.AGGREGATE_ONLY_SUBJECT))
                .orElseThrow();

        JwtTokenService.JwtValidationResult result = tokenService.validate(bearerToken(authorization));

        assertThat(result.valid()).isTrue();
        assertThat(result.authorities()).containsExactly(HrAnalyticsAuthorities.AGGREGATE_READ);
    }

    @Test
    void delegatesCanonicalAuthoritiesForTheConfiguredLocalReferencePrincipal() {
        String authorization = provider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/human-resources/vw-analytics-folha-pagamento/capabilities",
                "demo")).orElseThrow();

        JwtTokenService.JwtValidationResult result = tokenService.validate(bearerToken(authorization));

        assertThat(result.valid()).isTrue();
        assertThat(result.subject()).isEqualTo("demo");
        assertThat(result.authorities()).containsExactlyInAnyOrder(
                HrAnalyticsAuthorities.AGGREGATE_READ,
                HrAnalyticsAuthorities.NOMINAL_READ,
                HrAnalyticsAuthorities.EMPLOYEE_360_READ);
    }

    @Test
    void doesNotPromoteTheLocalReferencePrincipalInCorporateMode() {
        QuickstartGovernedPlatformRequestAuthorizationProvider corporateProvider =
                new QuickstartGovernedPlatformRequestAuthorizationProvider(
                        tokenService,
                        new QuickstartPrincipalAuthorityCatalog("admin", "demo", true));

        assertThat(corporateProvider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/human-resources/vw-analytics-folha-pagamento/capabilities",
                "demo"))).isEmpty();
    }

    @Test
    void failsClosedForUnknownPrincipalsCrossOriginTargetsAndUnexpectedPaths() {
        assertThat(provider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/human-resources/vw-analytics-afastamentos/capabilities",
                "unknown"))).isEmpty();
        assertThat(provider.authorizationHeader(request(
                "http://localhost:8088",
                "https://metadata.example/api/human-resources/vw-analytics-afastamentos/capabilities",
                "admin"))).isEmpty();
        assertThat(provider.authorizationHeader(request(
                "http://localhost:8088",
                "http://localhost:8088/api/internal/secrets",
                "admin"))).isEmpty();
    }

    private GovernedPlatformRequest request(String requestBaseUrl, String targetUrl, String userId) {
        return new GovernedPlatformRequest(
                GovernedPlatformRequest.Surface.RESOURCE_CAPABILITIES,
                URI.create(requestBaseUrl),
                URI.create(targetUrl),
                "desenv",
                userId,
                "local");
    }

    private String bearerToken(String authorization) {
        assertThat(authorization).startsWith("Bearer ");
        return authorization.substring("Bearer ".length());
    }
}
