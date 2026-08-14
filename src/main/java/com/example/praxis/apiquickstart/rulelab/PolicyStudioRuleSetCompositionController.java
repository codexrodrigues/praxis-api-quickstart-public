package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.praxisplatform.config.dto.DomainRuleCompositionApprovalResponse;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.config.contract.RuleSetCompositionAction;
import org.praxisplatform.config.contract.RuleSetCompositionCandidate;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateCommand;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateRequest;
import org.praxisplatform.config.contract.RuleSetCompositionPublication;
import org.praxisplatform.config.exception.DomainRuleSnapshotControlPlaneException;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipalResolver;
import org.praxisplatform.rules.plan.RulePlanException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Host-owned composition adapter used by Policy Studio without exposing executable graphs. */
@RestController
@RequestMapping("/api/praxis/policy-studio/rule-sets")
@RequiredArgsConstructor
public class PolicyStudioRuleSetCompositionController {
    private final PolicyStudioRuleSetCompositionService service;
    private final DomainRuleGovernancePrincipalResolver principalResolver;

    @PostMapping("/{ruleSetKey}/candidate")
    public ResponseEntity<RuleSetCompositionCandidate> prepare(
            @PathVariable String ruleSetKey,
            @RequestBody RuleSetCompositionCandidateRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenant,
            @RequestHeader(value = "X-Env", required = false) String environment,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        boolean publisher = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER.equals(
                        authority.getAuthority()));
        String requiredRole = publisher ? "RULE_SNAPSHOT_PUBLISHER" : "RULE_COMPOSITION_APPROVER";
        DomainRuleGovernancePrincipal principal = principalResolver.resolve(
                servletRequest, tenant, environment, requiredRole);
        List<RuleSetCompositionAction> authorizedActions = publisher
                ? List.of(RuleSetCompositionAction.PREPARE, RuleSetCompositionAction.PUBLISH)
                : List.of(RuleSetCompositionAction.PREPARE, RuleSetCompositionAction.APPROVE);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.prepare(ruleSetKey, request, principal, authorizedActions));
    }

    @PostMapping("/{ruleSetKey}/candidate/approvals")
    public ResponseEntity<DomainRuleCompositionApprovalResponse> approve(
            @PathVariable String ruleSetKey,
            @RequestBody RuleSetCompositionCandidateCommand command,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenant,
            @RequestHeader(value = "X-Env", required = false) String environment,
            HttpServletRequest servletRequest) {
        DomainRuleGovernancePrincipal principal = principalResolver.resolve(
                servletRequest, tenant, environment, "RULE_COMPOSITION_APPROVER");
        return ResponseEntity.ok(service.approve(ruleSetKey, command, principal));
    }

    @PostMapping("/{ruleSetKey}/candidate/publish")
    public ResponseEntity<RuleSetCompositionPublication> publish(
            @PathVariable String ruleSetKey,
            @RequestBody RuleSetCompositionCandidateCommand command,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenant,
            @RequestHeader(value = "X-Env", required = false) String environment,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            HttpServletRequest servletRequest) {
        DomainRuleGovernancePrincipal principal = principalResolver.resolve(
                servletRequest, tenant, environment, "RULE_SNAPSHOT_PUBLISHER");
        DomainRuleSnapshotActivationResponse response = service.publish(
                ruleSetKey, command, ifMatch, ifNoneMatch, principal);
        RuleSetCompositionPublication safe = new RuleSetCompositionPublication(
                response.snapshot().snapshotKey(),
                response.snapshot().ruleSet().ref().ruleSetKey(),
                response.snapshot().ruleSet().ref().version(),
                response.snapshotContentHash(),
                response.headEtag(),
                response.activationRevision(),
                PublishedRuleSnapshotHeadActivationType.valueOf(response.activationType()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + response.headEtag() + '"')
                .cacheControl(CacheControl.noCache())
                .body(safe);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidCandidate(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_RULESET_CANDIDATE",
                "message", exception.getMessage()));
    }

    @ExceptionHandler(StalePolicyStudioRuleSetCandidateException.class)
    public ResponseEntity<Map<String, String>> staleCandidate(
            StalePolicyStudioRuleSetCandidateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "STALE_RULESET_CANDIDATE",
                "message", exception.getMessage()));
    }

    @ExceptionHandler(DomainRuleSnapshotControlPlaneException.class)
    public ResponseEntity<Map<String, String>> controlPlaneFailure(
            DomainRuleSnapshotControlPlaneException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
                "code", exception.status().name(),
                "message", exception.getMessage()));
    }

    @ExceptionHandler(RulePlanException.class)
    public ResponseEntity<Map<String, String>> invalidPlan(RulePlanException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", exception.getCode().name(),
                "message", exception.getMessage()));
    }
}
