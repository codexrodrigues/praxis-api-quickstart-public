package com.example.praxis.apiquickstart.auth;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.COMPOSITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_AUTHOR;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_READER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_OPERATOR;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_READER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.OPERATIONAL_TEST_OPERATOR;

/**
 * Opt-in local identities used only to prove maker-checker HTTP behavior in the public lab.
 * Corporate hosts must replace the quickstart login with their IdP and IAM role mapping.
 */
@Component
public class GovernanceLabIdentityService {
    private final boolean enabled;
    private final String adminUsername;
    private final List<ConfiguredIdentity> identities;

    public GovernanceLabIdentityService(
            @Value("${app.auth.governance-lab.enabled:false}") boolean enabled,
            @Value("${spring.security.user.name:admin}") String adminUsername,
            @Value("${app.auth.governance-lab.author.username:}") String authorUsername,
            @Value("${app.auth.governance-lab.author.password:}") String authorPassword,
            @Value("${app.auth.governance-lab.approver-a.username:}") String approverAUsername,
            @Value("${app.auth.governance-lab.approver-a.password:}") String approverAPassword,
            @Value("${app.auth.governance-lab.approver-b.username:}") String approverBUsername,
            @Value("${app.auth.governance-lab.approver-b.password:}") String approverBPassword,
            @Value("${app.auth.governance-lab.publisher.username:}") String publisherUsername,
            @Value("${app.auth.governance-lab.publisher.password:}") String publisherPassword,
            @Value("${app.auth.governance-lab.operator.username:}") String operatorUsername,
            @Value("${app.auth.governance-lab.operator.password:}") String operatorPassword,
            @Value("${app.auth.governance-lab.auditor.username:}") String auditorUsername,
            @Value("${app.auth.governance-lab.auditor.password:}") String auditorPassword) {
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.identities = List.of(
                new ConfiguredIdentity("author", authorUsername, authorPassword, "GOVERNANCE_AUTHOR",
                        List.of(DEFINITION_READER, DEFINITION_AUTHOR, SNAPSHOT_READER)),
                new ConfiguredIdentity("approver-a", approverAUsername, approverAPassword, "GOVERNANCE_APPROVER",
                        List.of(DEFINITION_READER, DEFINITION_APPROVER, COMPOSITION_APPROVER, SNAPSHOT_READER)),
                new ConfiguredIdentity("approver-b", approverBUsername, approverBPassword, "GOVERNANCE_APPROVER",
                        List.of(DEFINITION_READER, DEFINITION_APPROVER, COMPOSITION_APPROVER, SNAPSHOT_READER)),
                new ConfiguredIdentity("publisher", publisherUsername, publisherPassword, "GOVERNANCE_PUBLISHER",
                        List.of(DEFINITION_READER, SNAPSHOT_PUBLISHER, SNAPSHOT_READER)),
                new ConfiguredIdentity("operator", operatorUsername, operatorPassword, "GOVERNANCE_OPERATOR",
                        List.of(DEFINITION_READER, SNAPSHOT_OPERATOR, SNAPSHOT_READER,
                                OPERATIONAL_TEST_OPERATOR)),
                new ConfiguredIdentity("auditor", auditorUsername, auditorPassword, "GOVERNANCE_AUDITOR",
                        List.of(DEFINITION_READER, SNAPSHOT_READER)));
    }

    @PostConstruct
    void validateConfiguration() {
        if (!enabled) {
            return;
        }
        if (identities.stream().anyMatch(identity -> !StringUtils.hasText(identity.username())
                || !StringUtils.hasText(identity.password()))) {
            throw new IllegalStateException("Governance lab identities require all configured usernames and passwords");
        }
        if (identities.stream().map(ConfiguredIdentity::username).collect(java.util.stream.Collectors.toSet()).size()
                != identities.size()) {
            throw new IllegalStateException("Governance lab identities must use six distinct usernames");
        }
        if (identities.stream().anyMatch(identity -> identity.username().equals(adminUsername))) {
            throw new IllegalStateException("Governance lab identities must be distinct from the quickstart administrator");
        }
    }

    public Optional<AuthenticatedIdentity> authenticate(String username, String password) {
        if (!enabled || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }
        return identities.stream()
                .filter(identity -> identity.username().equals(username)
                        && MessageDigest.isEqual(
                                identity.password().getBytes(StandardCharsets.UTF_8),
                                password.getBytes(StandardCharsets.UTF_8)))
                .map(identity -> new AuthenticatedIdentity(
                        identity.username(), identity.role(), Set.copyOf(identity.authorities())))
                .findFirst();
    }

    /**
     * Switches among the configured technical actors only inside the opt-in governance lab.
     *
     * <p>The current session must already belong to this exact lab. This is a development-host
     * convenience for demonstrating the real maker-checker HTTP lifecycle without exposing any
     * configured password to the browser. It is disabled by default and is not an IAM substitute.</p>
     */
    public Optional<AuthenticatedIdentity> switchIdentity(String identityKey, String currentSubject) {
        if (!enabled || !StringUtils.hasText(identityKey) || !StringUtils.hasText(currentSubject)
                || identities.stream().noneMatch(identity -> identity.username().equals(currentSubject))) {
            return Optional.empty();
        }
        return identities.stream()
                .filter(identity -> identity.key().equals(identityKey))
                .map(identity -> new AuthenticatedIdentity(
                        identity.username(), identity.role(), Set.copyOf(identity.authorities())))
                .findFirst();
    }

    private record ConfiguredIdentity(
            String key, String username, String password, String role, List<String> authorities) {
    }

    public record AuthenticatedIdentity(String subject, String role, Set<String> authorities) {
    }
}
