package com.example.praxis.apiquickstart.rulelab;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Resolves an exact canonical rule identity to one and only one executable host provider. */
final class PolicyStudioSandboxRuleSetRegistry {
    private final Map<String, PolicyStudioSandboxRuleSetProvider> providers;

    PolicyStudioSandboxRuleSetRegistry(Collection<PolicyStudioSandboxRuleSetProvider> providers) {
        Map<String, PolicyStudioSandboxRuleSetProvider> indexed = new LinkedHashMap<>();
        if (providers != null) {
            for (PolicyStudioSandboxRuleSetProvider provider : providers) {
                if (provider == null) continue;
                for (String ruleKey : provider.ruleKeys()) {
                    if (ruleKey == null || ruleKey.isBlank()
                            || indexed.putIfAbsent(ruleKey, provider) != null) {
                        throw new IllegalArgumentException(
                                "Policy Studio sandbox rule identities must be non-empty and unique");
                    }
                }
            }
        }
        this.providers = Map.copyOf(indexed);
    }

    PolicyStudioSandboxRuleSetProvider require(String ruleKey) {
        PolicyStudioSandboxRuleSetProvider provider = providers.get(ruleKey);
        if (provider == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No executable host RuleSet provider owns workspace ruleKey");
        }
        return provider;
    }
}
