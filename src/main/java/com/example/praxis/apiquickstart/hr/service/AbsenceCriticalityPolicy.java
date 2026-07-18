package com.example.praxis.apiquickstart.hr.service;

/**
 * Host-owned deterministic policy used by the HR absence analytics pilot.
 *
 * <p>The PostgreSQL function {@code hr_absence_criticality_level} is the single executable owner
 * of thresholds. These constants only attest the policy identity in public metadata.</p>
 */
public final class AbsenceCriticalityPolicy {

    public static final String POLICY_ID = "hr-absence-criticality-v1";
    public static final String POLICY_VERSION = "2026-07-15";

    private AbsenceCriticalityPolicy() {
    }
}
