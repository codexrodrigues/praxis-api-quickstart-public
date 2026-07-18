package com.example.praxis.apiquickstart.hr.security;

import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Applies server-resolved department scope before HR analytics queries execute. */
@Component("hrDepartmentScopeAccess")
public class HrDepartmentScopeAccess {
    private final HrDepartmentScopeProvider scopeProvider;

    public HrDepartmentScopeAccess(HrDepartmentScopeProvider scopeProvider) {
        this.scopeProvider = scopeProvider;
    }

    public <F extends HrDepartmentScopedFilter> F applyAnalyticsScope(F filter, Supplier<F> emptyFilterFactory) {
        F effectiveFilter = filter == null ? emptyFilterFactory.get() : filter;
        Optional<Set<Integer>> scope = currentScope();
        if (scope.isEmpty()) {
            return effectiveFilter;
        }
        Set<Integer> allowed = scope.orElseThrow();
        if (allowed.isEmpty()) {
            throw forbidden("No HR departments are authorized for the current principal.");
        }
        Integer requestedDepartment = effectiveFilter.getDepartamentoId();
        if (requestedDepartment != null && !allowed.contains(requestedDepartment)) {
            throw forbidden("Requested department is outside the authorized HR scope.");
        }
        List<Integer> requested = effectiveFilter.getDepartamentoIdsIn();
        List<Integer> effectiveDepartments = requested == null || requested.isEmpty()
                ? allowed.stream().sorted().toList()
                : requested.stream().filter(allowed::contains).distinct().sorted().toList();
        if (effectiveDepartments.isEmpty()) {
            throw forbidden("Requested departments are outside the authorized HR scope.");
        }
        effectiveFilter.setDepartamentoIdsIn(effectiveDepartments);
        return effectiveFilter;
    }

    /**
     * Applies the row scope accepted by aggregate analytics without allowing an aggregate-only
     * principal to turn a department comparison into a one-person inference query.
     */
    public <F extends HrDepartmentScopedFilter> F applyAggregateComparisonScope(
            F filter,
            Supplier<F> emptyFilterFactory
    ) {
        if (filter != null
                && filter.getFuncionarioIdsIn() != null
                && !filter.getFuncionarioIdsIn().isEmpty()
                && !hasAuthority(HrAnalyticsAuthorities.NOMINAL_READ)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Employee filters require nominal HR analytics authority.");
        }
        return applyAnalyticsScope(filter, emptyFilterFactory);
    }

    public void requireDepartment(Integer departmentId) {
        if (departmentId == null || currentScope().map(scope -> !scope.contains(departmentId)).orElse(false)) {
            throw forbidden("Department is outside the authorized HR scope.");
        }
    }

    public boolean isUnscoped(Authentication authentication) {
        return scopeFor(authentication).isEmpty();
    }

    public void requireUnscoped() {
        if (!isUnscoped(currentAuthentication())) {
            throw forbidden("Use the scoped filter endpoint for department-restricted HR analytics.");
        }
    }

    public void requireNominalRead() {
        if (!hasAuthority(HrAnalyticsAuthorities.NOMINAL_READ)) {
            throw forbidden("Nominal HR analytics authority is required for option sources.");
        }
    }

    /**
     * Resolves the canonical row-access boundary for nominal analytics resource filters.
     * Functional filter fields remain client-controlled; this scope is derived exclusively from
     * the authenticated principal and is therefore also safe for selected-ID rehydration.
     */
    public <E> ResourceFilterAccessScope<E> resolveAnalyticsResourceFilterAccessScope() {
        requireNominalRead();
        Optional<Set<Integer>> scope = currentScope();
        if (scope.isEmpty()) {
            return ResourceFilterAccessScope.unrestricted();
        }
        Set<Integer> allowed = scope.orElseThrow();
        if (allowed.isEmpty()) {
            return ResourceFilterAccessScope.denied();
        }
        return ResourceFilterAccessScope.restricted((root, query, criteriaBuilder) ->
                root.get("departamentoId").in(allowed));
    }

    private Optional<Set<Integer>> currentScope() {
        return scopeFor(currentAuthentication());
    }

    private Optional<Set<Integer>> scopeFor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated principal required.");
        }
        return scopeProvider.departmentsFor(authentication.getName()).map(Set::copyOf);
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private boolean hasAuthority(String requiredAuthority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(authority -> requiredAuthority.equals(authority.getAuthority()));
    }
}
