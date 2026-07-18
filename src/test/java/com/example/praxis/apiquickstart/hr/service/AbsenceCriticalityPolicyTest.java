package com.example.praxis.apiquickstart.hr.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbsenceCriticalityPolicyTest {

    @Test
    void attestsTheVersionedPolicyPublishedByThePostgreSqlMaterialization() {
        assertEquals("hr-absence-criticality-v1", AbsenceCriticalityPolicy.POLICY_ID);
        assertEquals("2026-07-15", AbsenceCriticalityPolicy.POLICY_VERSION);
    }
}
