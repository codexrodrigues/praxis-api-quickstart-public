package com.example.praxis.apiquickstart.hr.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supplies a deterministic, replaceable entitlement catalog for the Quickstart demo.
 *
 * <p>Department IDs use {@code |}; {@code *} explicitly grants an unbounded scope. Unknown
 * subjects resolve to deny-all, while the configured Quickstart admin is unbounded unless it has
 * an explicit entry. Corporate hosts replace this bean with their IAM/entitlements provider.</p>
 */
@Configuration
public class HrAnalyticsScopeConfiguration {

    @Bean
    @ConditionalOnMissingBean(HrDepartmentScopeProvider.class)
    HrDepartmentScopeProvider demoHrDepartmentScopeProvider(
            @Value("${app.hr.analytics.demo-department-scopes:demo-manager=1}") String configuredScopes,
            @Value("${spring.security.user.name:admin}") String adminSubject
    ) {
        Map<String, Set<Integer>> scopes = new LinkedHashMap<>();
        Set<String> unboundedSubjects = new LinkedHashSet<>();
        for (String entry : configuredScopes.split(",")) {
            String[] subjectAndDepartments = entry.trim().split("=", 2);
            if (subjectAndDepartments.length != 2 || subjectAndDepartments[0].isBlank()) {
                continue;
            }
            String subject = subjectAndDepartments[0].trim();
            if ("*".equals(subjectAndDepartments[1].trim())) {
                unboundedSubjects.add(subject);
                continue;
            }
            Set<Integer> departments = Arrays.stream(subjectAndDepartments[1].split("\\|"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
            scopes.put(subject, departments);
        }
        if (adminSubject != null
                && !adminSubject.isBlank()
                && !scopes.containsKey(adminSubject.trim())
                && !unboundedSubjects.contains(adminSubject.trim())) {
            unboundedSubjects.add(adminSubject.trim());
        }
        Map<String, Set<Integer>> immutableScopes = Map.copyOf(scopes);
        Set<String> immutableUnboundedSubjects = Set.copyOf(unboundedSubjects);
        return subject -> immutableUnboundedSubjects.contains(subject)
                ? Optional.empty()
                : Optional.of(immutableScopes.getOrDefault(subject, Set.of()));
    }
}
