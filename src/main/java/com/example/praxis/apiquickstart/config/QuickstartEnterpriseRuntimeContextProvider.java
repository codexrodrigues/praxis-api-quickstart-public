package com.example.praxis.apiquickstart.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextRequest;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextSwitchCommand;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextSwitchResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeNavigationNode;
import org.praxisplatform.config.dto.EnterpriseRuntimeNavigationResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeSecurityEvent;
import org.praxisplatform.config.dto.EnterpriseRuntimeSecurityEventsResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeTenant;
import org.praxisplatform.config.dto.EnterpriseRuntimeTenantsResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeUser;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.EnterpriseRuntimeContextProvider;
import org.praxisplatform.config.service.EnterpriseRuntimeContextSwitchProvider;
import org.praxisplatform.config.service.EnterpriseRuntimeNavigationProvider;
import org.praxisplatform.config.service.EnterpriseRuntimeSecurityEventProvider;
import org.praxisplatform.config.service.EnterpriseRuntimeTenantProvider;
import org.springframework.stereotype.Component;

/**
 * Host-owned enterprise runtime context projection used by the public quickstart.
 *
 * <p>This provider proves the {@code praxis-config-starter} SPI without depending on Ergon,
 * HADES or any private authorization store. Real corporate hosts should replace this demo
 * projection with a provider backed by their authenticated principal and tenant entitlement
 * service, keeping private roles and policies outside the public payload.</p>
 */
@Component
public class QuickstartEnterpriseRuntimeContextProvider
        implements EnterpriseRuntimeContextProvider,
                EnterpriseRuntimeContextSwitchProvider,
                EnterpriseRuntimeTenantProvider,
                EnterpriseRuntimeNavigationProvider,
                EnterpriseRuntimeSecurityEventProvider {

    private static final String SCHEMA_VERSION = "praxis-enterprise-runtime-context.v1";
    private static final String SWITCH_SCHEMA_VERSION = "praxis-enterprise-runtime-context-switch.v1";
    private static final String TENANTS_SCHEMA_VERSION = "praxis-enterprise-runtime-tenants.v1";
    private static final String NAVIGATION_SCHEMA_VERSION = "praxis-enterprise-runtime-navigation.v1";
    private static final String SECURITY_EVENTS_SCHEMA_VERSION = "praxis-enterprise-runtime-security-events.v1";

    @Override
    public EnterpriseRuntimeContextResponse getContext(EnterpriseRuntimeContextRequest request) {
        AiPrincipalContext principal = request != null ? request.principalContext() : null;
        String tenantId = valueOrDefault(principal != null ? principal.tenantId() : null, "desenv");
        String userId = valueOrDefault(principal != null ? principal.userId() : null, "demo");
        String environment = valueOrDefault(principal != null ? principal.environment() : null, "local");

        return new EnterpriseRuntimeContextResponse(
                SCHEMA_VERSION,
                new EnterpriseRuntimeUser(
                        userId,
                        "Praxis demo user",
                        principal != null && principal.resolvedFromServerPrincipal()),
                new EnterpriseRuntimeTenant(
                        tenantId,
                        "Praxis demo tenant",
                        true),
                environment,
                request != null ? request.locale() : null,
                request != null ? request.timezone() : null,
                request != null ? request.activeProfileId() : null,
                request != null ? request.activeModuleKey() : null,
                List.of(
                        "runtime.context.read",
                        "runtime.context.demo-provider"),
                Instant.now());
    }

    @Override
    public EnterpriseRuntimeContextSwitchResponse switchContext(
            EnterpriseRuntimeContextRequest currentRequest,
            EnterpriseRuntimeContextSwitchCommand command) {
        AiPrincipalContext principal = currentRequest != null ? currentRequest.principalContext() : null;
        String currentTenantId = valueOrDefault(principal != null ? principal.tenantId() : null, "desenv");
        String targetTenantId = valueOrDefault(command != null ? command.targetTenantId() : null, currentTenantId);
        boolean accepted = isDemoTenant(targetTenantId);
        String effectiveTenantId = accepted ? targetTenantId : currentTenantId;
        String environment = valueOrDefault(principal != null ? principal.environment() : null, "local");

        EnterpriseRuntimeContextResponse effectiveContext = new EnterpriseRuntimeContextResponse(
                SCHEMA_VERSION,
                new EnterpriseRuntimeUser(
                        valueOrDefault(principal != null ? principal.userId() : null, "demo"),
                        "Praxis demo user",
                        principal != null && principal.resolvedFromServerPrincipal()),
                new EnterpriseRuntimeTenant(
                        effectiveTenantId,
                        labelForTenant(effectiveTenantId),
                        true),
                environment,
                firstNonBlank(command != null ? command.locale() : null, currentRequest != null ? currentRequest.locale() : null),
                firstNonBlank(command != null ? command.timezone() : null, currentRequest != null ? currentRequest.timezone() : null),
                firstNonBlank(
                        command != null ? command.targetProfileId() : null,
                        currentRequest != null ? currentRequest.activeProfileId() : null),
                firstNonBlank(
                        command != null ? command.targetModuleKey() : null,
                        currentRequest != null ? currentRequest.activeModuleKey() : null),
                List.of(
                        "runtime.context.read",
                        "runtime.context.demo-provider"),
                Instant.now());

        return new EnterpriseRuntimeContextSwitchResponse(
                SWITCH_SCHEMA_VERSION,
                accepted,
                accepted
                        ? "Quickstart demo context switch accepted."
                        : "Quickstart demo context switch denied for unknown tenant.",
                effectiveContext,
                propagationHeaders(effectiveContext),
                accepted
                        ? List.of("runtime.context.switch", "runtime.context.switch.demo-provider")
                        : List.of("runtime.context.switch.denied", "runtime.context.switch.demo-provider"),
                Instant.now());
    }

    @Override
    public EnterpriseRuntimeTenantsResponse getTenants(EnterpriseRuntimeContextRequest request) {
        AiPrincipalContext principal = request != null ? request.principalContext() : null;
        String tenantId = valueOrDefault(principal != null ? principal.tenantId() : null, "desenv");
        EnterpriseRuntimeTenant activeTenant = new EnterpriseRuntimeTenant(
                tenantId,
                "Praxis demo tenant",
                true);

        return new EnterpriseRuntimeTenantsResponse(
                TENANTS_SCHEMA_VERSION,
                activeTenant,
                List.of(
                        activeTenant,
                        new EnterpriseRuntimeTenant("corporate-holding", "Corporate holding", false),
                        new EnterpriseRuntimeTenant("shared-services", "Shared services", false)),
                List.of(
                        "runtime.tenants.read",
                        "runtime.tenants.demo-provider"),
                Instant.now());
    }

    @Override
    public EnterpriseRuntimeNavigationResponse getNavigation(EnterpriseRuntimeContextRequest request) {
        EnterpriseRuntimeNavigationNode payrollRuns = new EnterpriseRuntimeNavigationNode(
                "payroll.folhas-pagamento",
                "Folhas de pagamento",
                "resource",
                "/api/human-resources/folhas-pagamento",
                "/payroll/folhas-pagamento",
                "payroll",
                "human-resources.folhas-pagamento",
                "table",
                null,
                "resource.read",
                List.of());

        EnterpriseRuntimeNavigationNode payrollApprovals = new EnterpriseRuntimeNavigationNode(
                "payroll.folhas-pagamento.aprovacoes",
                "Aprovacoes de folha",
                "action",
                "/api/human-resources/folhas-pagamento/actions/approve",
                "/payroll/folhas-pagamento/approvals",
                "payroll",
                "human-resources.folhas-pagamento",
                "detail",
                "approve",
                "resource.action.approve",
                List.of());

        EnterpriseRuntimeNavigationNode payrollModule = new EnterpriseRuntimeNavigationNode(
                "payroll",
                "Payroll",
                "module",
                null,
                "/payroll",
                "payroll",
                null,
                null,
                null,
                null,
                List.of(payrollRuns, payrollApprovals));

        return new EnterpriseRuntimeNavigationResponse(
                NAVIGATION_SCHEMA_VERSION,
                List.of(payrollModule),
                List.of(
                        "runtime.navigation.read",
                        "runtime.navigation.demo-provider"),
                Instant.now());
    }

    @Override
    public EnterpriseRuntimeSecurityEventsResponse getSecurityEvents(EnterpriseRuntimeContextRequest request) {
        AiPrincipalContext principal = request != null ? request.principalContext() : null;
        String tenantId = valueOrDefault(principal != null ? principal.tenantId() : null, "desenv");
        String environment = valueOrDefault(principal != null ? principal.environment() : null, "local");

        return new EnterpriseRuntimeSecurityEventsResponse(
                SECURITY_EVENTS_SCHEMA_VERSION,
                List.of(
                        new EnterpriseRuntimeSecurityEvent(
                                "quickstart.session.fresh",
                                "session.fresh",
                                "info",
                                "Demo session is active for public runtime exploration.",
                                tenantId,
                                environment,
                                Instant.now(),
                                Map.of("authPosture", "demo-public-read")),
                        new EnterpriseRuntimeSecurityEvent(
                                "quickstart.context.read-open",
                                "runtime.read_open",
                                "info",
                                "Read-open mode is enabled for quickstart runtime surfaces.",
                                tenantId,
                                environment,
                                Instant.now(),
                                Map.of("scope", "quickstart-demo"))),
                List.of(
                        "runtime.security-events.read",
                        "runtime.security-events.demo-provider"),
                Instant.now());
    }

    private Map<String, String> propagationHeaders(EnterpriseRuntimeContextResponse context) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Tenant-ID", context.activeTenant().tenantId());
        headers.put("X-Env", context.environment());
        putIfPresent(headers, "X-Praxis-Profile-ID", context.activeProfileId());
        putIfPresent(headers, "X-Praxis-Module-Key", context.activeModuleKey());
        putIfPresent(headers, "X-Timezone", context.timezone());
        return headers;
    }

    private void putIfPresent(Map<String, String> headers, String name, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            headers.put(name, normalized);
        }
    }

    private boolean isDemoTenant(String tenantId) {
        return "tenant-demo".equals(tenantId)
                || "desenv".equals(tenantId)
                || "corporate-holding".equals(tenantId)
                || "shared-services".equals(tenantId);
    }

    private String labelForTenant(String tenantId) {
        return switch (tenantId) {
            case "corporate-holding" -> "Corporate holding";
            case "shared-services" -> "Shared services";
            default -> "Praxis demo tenant";
        };
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalized = normalize(preferred);
        return normalized != null ? normalized : normalize(fallback);
    }

    private String valueOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized != null ? normalized : fallback;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
