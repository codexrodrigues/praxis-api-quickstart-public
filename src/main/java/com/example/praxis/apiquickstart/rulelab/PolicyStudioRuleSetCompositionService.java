package com.example.praxis.apiquickstart.rulelab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.praxisplatform.config.dto.DomainRuleCompositionApprovalResponse;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestRequest;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.config.contract.RuleSetCompositionAction;
import org.praxisplatform.config.contract.RuleSetCompositionCandidate;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateCommand;
import org.praxisplatform.config.contract.RuleSetCompositionCandidateRequest;
import org.praxisplatform.config.contract.RuleSetCompositionSource;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;

/**
 * Host adapter that composes the complete executable graph and delegates every governed mutation
 * to the Config control plane. Conditions and executors never cross the browser boundary.
 */
public class PolicyStudioRuleSetCompositionService {
    private static final Set<String> PUBLISHABLE = Set.of("approved", "active");

    private final DomainRuleService domainRules;
    private final DomainRuleSnapshotService snapshots;

    public PolicyStudioRuleSetCompositionService(
            DomainRuleService domainRules,
            DomainRuleSnapshotService snapshots) {
        this.domainRules = domainRules;
        this.snapshots = snapshots;
    }

    public RuleSetCompositionCandidate prepare(
            String ruleSetKey,
            RuleSetCompositionCandidateRequest request,
            DomainRuleGovernancePrincipal principal,
            List<RuleSetCompositionAction> authorizedActions) {
        PreparedCandidate prepared = prepareCandidate(ruleSetKey, request, principal);
        RuleSetCompositionCandidate response = prepared.response();
        return new RuleSetCompositionCandidate(
                response.ruleSetKey(), response.ruleSetVersion(), response.compositionDigest(),
                response.implementationCatalogDigest(), response.currentHeadEtag(),
                response.sources(), authorizedActions);
    }

    public DomainRuleCompositionApprovalResponse approve(
            String ruleSetKey,
            RuleSetCompositionCandidateCommand command,
            DomainRuleGovernancePrincipal principal) {
        PreparedCandidate prepared = prepareCandidate(ruleSetKey, request(command), principal);
        requireExpectedDigest(command.expectedCompositionDigest(), prepared.response().compositionDigest());
        return snapshots.approveComposition(
                prepared.manifest(), principal.tenantId(), principal.environment(), principal.actorRef());
    }

    public DomainRuleSnapshotActivationResponse publish(
            String ruleSetKey,
            RuleSetCompositionCandidateCommand command,
            String ifMatch,
            String ifNoneMatch,
            DomainRuleGovernancePrincipal principal) {
        PreparedCandidate prepared = prepareCandidate(ruleSetKey, request(command), principal);
        requireExpectedDigest(command.expectedCompositionDigest(), prepared.response().compositionDigest());
        return snapshots.publish(
                new org.praxisplatform.config.dto.DomainRuleSnapshotPublicationRequest(
                        prepared.manifest().ruleSet(),
                        prepared.manifest().sourceDefinitionIds(),
                        prepared.manifest().ownerServiceKey(),
                        prepared.manifest().requiredHostContractVersion(),
                        prepared.manifest().validFromUtc(),
                        prepared.manifest().validUntilUtc(),
                        prepared.response().compositionDigest()),
                principal.tenantId(), principal.environment(), ifMatch, ifNoneMatch, principal.actorRef());
    }

    private PreparedCandidate prepareCandidate(
            String requestedRuleSetKey,
            RuleSetCompositionCandidateRequest request,
            DomainRuleGovernancePrincipal principal) {
        if (request == null || request.promotedDefinitionId() == null) {
            throw new IllegalArgumentException("promotedDefinitionId is required");
        }
        String ruleSetKey = requireText(requestedRuleSetKey, "ruleSetKey");
        if (!ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY.equals(ruleSetKey)) {
            throw new IllegalArgumentException("RuleSet is not owned by this host composer");
        }

        List<DomainRuleDefinitionResponse> definitions = domainRules.definitions(
                principal.tenantId(), principal.environment(), ruleSetKey, null, null, null);
        DomainRuleDefinitionResponse promoted = definitions.stream()
                .filter(item -> request.promotedDefinitionId().equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Promoted definition was not found in this scoped RuleSet"));
        if (!PUBLISHABLE.contains(promoted.status())) {
            throw new IllegalArgumentException("Promoted definition must be approved or active");
        }

        var governedBindings = ExtraordinaryGrantRuleSetComposer
                .governedBindings(ExtraordinaryGrantRuleSetFactory.definition());
        if (governedBindings.stream().noneMatch(binding -> binding.bindingKey().equals(promoted.ruleKey()))) {
            throw new IllegalArgumentException("Promoted definition is not a governed binding of this RuleSet");
        }
        if (!"REFERENCE_DRAFT_ONLY".equals(
                promoted.governance() == null
                        ? null
                        : promoted.governance().path("lifecycleBoundary").asText(null))) {
            throw new IllegalArgumentException("Promoted definition is not governed as a RuleSet source");
        }

        List<DomainRuleDefinitionResponse> sources = governedBindings.stream()
                .map(binding -> selectSource(binding.bindingKey(), promoted, definitions))
                .toList();
        var head = snapshots.findHeadStatus(
                principal.tenantId(), principal.environment(), ruleSetKey);
        int candidateVersion = head
                .map(status -> status.ruleSetVersion() + 1)
                .orElse(1);
        ExtraordinaryGrantRuleSetComposer.SnapshotCandidate candidate =
                ExtraordinaryGrantRuleSetComposer.compose(candidateVersion, sources);
        DomainRuleCompositionManifestRequest manifest = new DomainRuleCompositionManifestRequest(
                candidate.ruleSet(),
                candidate.sourceDefinitionIds(),
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                requireText(request.validFromUtc(), "validFromUtc"),
                normalize(request.validUntilUtc()));
        var canonical = snapshots.prepareCompositionManifest(
                manifest, principal.tenantId(), principal.environment());
        String headEtag = head.map(status -> status.headEtag()).orElse(null);
        List<RuleSetCompositionSource> safeSources = sources.stream()
                .map(source -> new RuleSetCompositionSource(
                        source.id(), source.ruleKey(), source.version(), source.status()))
                .toList();
        return new PreparedCandidate(manifest, new RuleSetCompositionCandidate(
                ruleSetKey,
                candidateVersion,
                canonical.compositionDigest(),
                canonical.implementationCatalogDigest(),
                headEtag,
                safeSources,
                List.of()));
    }

    private DomainRuleDefinitionResponse selectSource(
            String ruleKey,
            DomainRuleDefinitionResponse promoted,
            List<DomainRuleDefinitionResponse> definitions) {
        if (ruleKey.equals(promoted.ruleKey())) return promoted;
        return definitions.stream()
                .filter(item -> ruleKey.equals(item.ruleKey()))
                .filter(item -> PUBLISHABLE.contains(item.status()))
                .max(Comparator.comparing(DomainRuleDefinitionResponse::version))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Complete approved RuleSet coverage is required; missing source " + ruleKey));
    }

    private RuleSetCompositionCandidateRequest request(RuleSetCompositionCandidateCommand command) {
        if (command == null) throw new IllegalArgumentException("candidate command is required");
        return command.candidateRequest();
    }

    private void requireExpectedDigest(String expected, String actual) {
        String value = requireText(expected, "expectedCompositionDigest");
        if (!MessageDigest.isEqual(
                value.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII))) {
            throw new StalePolicyStudioRuleSetCandidateException(
                    "Composition changed after inspection; prepare a new candidate");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PreparedCandidate(
            DomainRuleCompositionManifestRequest manifest,
            RuleSetCompositionCandidate response) {
    }
}
