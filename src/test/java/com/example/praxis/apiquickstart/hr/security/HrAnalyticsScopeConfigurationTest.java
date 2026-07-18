package com.example.praxis.apiquickstart.hr.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HrAnalyticsScopeConfigurationTest {

    @Test
    void shouldDenyUnknownSubjectsAndKeepUnboundedSubjectsExplicit() {
        HrDepartmentScopeProvider provider = new HrAnalyticsScopeConfiguration()
                .demoHrDepartmentScopeProvider(
                        "demo-manager=1|3,hr-analyst=*,blocked=",
                        "admin");

        assertEquals(Optional.of(Set.of(1, 3)), provider.departmentsFor("demo-manager"));
        assertTrue(provider.departmentsFor("hr-analyst").isEmpty());
        assertTrue(provider.departmentsFor("admin").isEmpty());
        assertEquals(Optional.of(Set.of()), provider.departmentsFor("blocked"));
        assertEquals(Optional.of(Set.of()), provider.departmentsFor("unknown-user"));
    }

    @Test
    void shouldHonorAnExplicitAdminScopeInsteadOfMakingItUnbounded() {
        HrDepartmentScopeProvider provider = new HrAnalyticsScopeConfiguration()
                .demoHrDepartmentScopeProvider("admin=2", "admin");

        assertEquals(Optional.of(Set.of(2)), provider.departmentsFor("admin"));
    }
}
