package com.example.praxis.apiquickstart.hr.security;

/** Stable domain authorities for the HR analytics corporate pilot. */
public final class HrAnalyticsAuthorities {
    public static final String AGGREGATE_READ = "HR_ANALYTICS_AGGREGATE_READ";
    public static final String NOMINAL_READ = "HR_ANALYTICS_NOMINAL_READ";
    public static final String EMPLOYEE_360_READ = "HR_EMPLOYEE_360_READ";

    private HrAnalyticsAuthorities() {
    }
}
