package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.praxisplatform.config.contract.DomainRuleTestRunResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;

/** Host-internal boundary between the HTTP command and operational proof executor. */
interface PolicyStudioOperationalTestRunPort {
    Optional<DomainRuleTestRunResponse> existingSandbox(
            PolicyStudioSandboxRunRequest request,
            DomainRuleGovernancePrincipal principal);

    DomainRuleTestRunResponse executeSandbox(
            PolicyStudioSandboxRunRequest request,
            Function<PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun,
                    List<ExtraordinaryBenefitOperationalScenarioBinding>> bindingFactory,
            Set<String> permissions,
            String actorSubject,
            String correlationId,
            DomainRuleGovernancePrincipal principal);
}
