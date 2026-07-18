package com.example.praxis.apiquickstart.hr.security;

import com.example.praxis.apiquickstart.hr.dto.filter.VwAnalyticsAfastamentoFilterDTO;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HrDepartmentScopeAccessTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIntersectRequestedDepartmentsWithServerResolvedScope() {
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject ->
                Optional.of(Set.of(1, 3))
        );
        authenticate("demo-manager");
        VwAnalyticsAfastamentoFilterDTO filter = new VwAnalyticsAfastamentoFilterDTO();
        filter.setDepartamentoIdsIn(List.of(2, 3, 3));

        VwAnalyticsAfastamentoFilterDTO scoped = access.applyAnalyticsScope(
                filter, VwAnalyticsAfastamentoFilterDTO::new);

        assertEquals(List.of(3), scoped.getDepartamentoIdsIn());
    }

    @Test
    void shouldApplyAllAllowedDepartmentsWhenClientDoesNotChooseOne() {
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of(3, 1)));
        authenticate("demo-manager");

        VwAnalyticsAfastamentoFilterDTO scoped = access.applyAnalyticsScope(
                new VwAnalyticsAfastamentoFilterDTO(), VwAnalyticsAfastamentoFilterDTO::new);

        assertEquals(List.of(1, 3), scoped.getDepartamentoIdsIn());
    }

    @Test
    void shouldFailClosedForDepartmentOutsideScopeAndDenyUnscopedEndpoints() {
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of(1)));
        authenticate("demo-manager");

        ResponseStatusException departmentFailure = assertThrows(ResponseStatusException.class,
                () -> access.requireDepartment(2));
        ResponseStatusException unscopedFailure = assertThrows(ResponseStatusException.class,
                access::requireUnscoped);

        assertEquals(HttpStatus.FORBIDDEN, departmentFailure.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, unscopedFailure.getStatusCode());
    }

    @Test
    void shouldFailClosedWhenPrincipalHasNoDepartmentsOrIntersectionIsEmpty() {
        HrDepartmentScopeAccess denyAll = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of()));
        authenticate("unknown-user");

        ResponseStatusException emptyScopeFailure = assertThrows(ResponseStatusException.class,
                () -> denyAll.applyAnalyticsScope(
                        new VwAnalyticsAfastamentoFilterDTO(), VwAnalyticsAfastamentoFilterDTO::new));

        HrDepartmentScopeAccess departmentOne = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of(1)));
        VwAnalyticsAfastamentoFilterDTO outsideScope = new VwAnalyticsAfastamentoFilterDTO();
        outsideScope.setDepartamentoIdsIn(List.of(2));
        ResponseStatusException emptyIntersectionFailure = assertThrows(ResponseStatusException.class,
                () -> departmentOne.applyAnalyticsScope(outsideScope, VwAnalyticsAfastamentoFilterDTO::new));

        assertEquals(HttpStatus.FORBIDDEN, emptyScopeFailure.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, emptyIntersectionFailure.getStatusCode());
    }

    @Test
    void shouldRejectScalarDepartmentOutsideServerResolvedScope() {
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of(1)));
        authenticate("demo-manager");
        VwAnalyticsAfastamentoFilterDTO filter = new VwAnalyticsAfastamentoFilterDTO();
        filter.setDepartamentoId(2);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> access.applyAnalyticsScope(filter, VwAnalyticsAfastamentoFilterDTO::new));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test
    void shouldTreatMissingScopeAsUnboundedForCorporateAnalyst() {
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject -> Optional.empty());
        authenticate("hr-analyst");

        assertTrue(access.isUnscoped(SecurityContextHolder.getContext().getAuthentication()));
        access.requireDepartment(99);
        access.requireUnscoped();
        assertNull(access.applyAnalyticsScope(
                new VwAnalyticsAfastamentoFilterDTO(), VwAnalyticsAfastamentoFilterDTO::new).getDepartamentoIdsIn());
    }

    @Test
    void shouldResolveExplicitResourceFilterAccessModesFromAuthenticatedScope() {
        authenticate("hr-analyst", HrAnalyticsAuthorities.NOMINAL_READ);
        HrDepartmentScopeAccess global = new HrDepartmentScopeAccess(subject -> Optional.empty());
        assertEquals(ResourceFilterAccessScope.Mode.UNRESTRICTED,
                global.resolveAnalyticsResourceFilterAccessScope().mode());

        HrDepartmentScopeAccess denied = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of()));
        assertEquals(ResourceFilterAccessScope.Mode.DENIED,
                denied.resolveAnalyticsResourceFilterAccessScope().mode());

        HrDepartmentScopeAccess restricted = new HrDepartmentScopeAccess(subject -> Optional.of(Set.of(1, 3)));
        ResourceFilterAccessScope<Object> scope = restricted.resolveAnalyticsResourceFilterAccessScope();
        assertEquals(ResourceFilterAccessScope.Mode.RESTRICTED, scope.mode());
        assertNotNull(scope.specification());
    }

    @Test
    void shouldRequireNominalAuthorityBeforeResolvingResourceFilterAccess() {
        authenticate("aggregate-reader");
        HrDepartmentScopeAccess access = new HrDepartmentScopeAccess(subject -> Optional.empty());

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                access::resolveAnalyticsResourceFilterAccessScope);

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    private void authenticate(String subject, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        subject,
                        "n/a",
                        java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList())
        );
    }
}
