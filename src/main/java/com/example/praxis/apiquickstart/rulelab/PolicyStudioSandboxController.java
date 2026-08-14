package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipalResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Host-owned, read-only sandbox entry point for Policy Studio. */
@RestController
@RequestMapping("/api/praxis/policy-studio/sandbox")
@RequiredArgsConstructor
public class PolicyStudioSandboxController {
    private final PolicyStudioSandboxService service;
    private final DomainRuleGovernancePrincipalResolver principalResolver;

    @PostMapping("/runs")
    public ResponseEntity<PolicyStudioSandboxRunResponse> run(
            @RequestBody PolicyStudioSandboxRunRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenant,
            @RequestHeader(value = "X-Env", required = false) String environment,
            HttpServletRequest servletRequest) {
        DomainRuleGovernancePrincipal principal = principalResolver.resolve(
                servletRequest, tenant, environment, "RULE_DEFINITION_AUTHOR");
        return ResponseEntity.ok(service.run(request, principal));
    }
}
