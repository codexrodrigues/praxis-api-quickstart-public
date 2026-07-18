package com.example.praxis.apiquickstart.hr.security;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves department entitlements on the server. An empty optional means the principal has an
 * unbounded HR scope; an empty set is a deliberate deny-all scope.
 */
public interface HrDepartmentScopeProvider {
    Optional<Set<Integer>> departmentsFor(String principalSubject);
}
