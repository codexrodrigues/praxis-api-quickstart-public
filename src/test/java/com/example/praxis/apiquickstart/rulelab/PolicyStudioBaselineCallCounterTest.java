package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PolicyStudioBaselineCallCounterTest {

    @Test
    void measuresObservedCallsAndClearsThePerThreadScope() {
        PolicyStudioBaselineCallCounter counter = new PolicyStudioBaselineCallCounter();
        int baseline = counter.current();

        assertEquals("legacy-result", counter.observeBaselineCall(() -> "legacy-result"));
        assertEquals(1, counter.deltaSince(baseline));

        counter.clear();
        assertEquals(0, counter.current());
        assertEquals(0, counter.deltaSince(0));
    }
}
