package com.example.praxis.apiquickstart.config;

import java.time.Instant;
import java.util.List;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextRequest;
import org.praxisplatform.config.dto.EnterpriseRuntimeContextResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeNavigationNode;
import org.praxisplatform.config.dto.EnterpriseRuntimeNavigationResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeTenant;
import org.praxisplatform.config.dto.EnterpriseRuntimeTenantsResponse;
import org.praxisplatform.config.dto.EnterpriseRuntimeUser;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.EnterpriseRuntimeContextProvider;
import org.praxisplatform.config.service.EnterpriseRuntimeNavigationProvider;
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
        implements EnterpriseRuntimeContextProvider, EnterpriseRuntimeTenantProvider, EnterpriseRuntimeNavigationProvider {

    private static final String SCHEMA_VERSION = "praxis-enterprise-runtime-context.v1";
    private static final String TENANTS_SCHEMA_VERSION = "praxis-enterprise-runtime-tenants.v1";
    private static final String NAVIGATION_SCHEMA_VERSION = "praxis-enterprise-runtime-navigation.v1";

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

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
