package com.example.praxis.apiquickstart.rulelab;

/**
 * Host adapter for external statement events.
 *
 * <p>Implementations must deduplicate by {@code messageId}. The dispatcher provides at-least-once
 * delivery and deliberately does not claim distributed exactly-once semantics.</p>
 */
public interface ExtraordinaryBenefitStatementEventSink {
    void deliver(ExtraordinaryBenefitStatementOutboxDelivery delivery) throws Exception;
}
