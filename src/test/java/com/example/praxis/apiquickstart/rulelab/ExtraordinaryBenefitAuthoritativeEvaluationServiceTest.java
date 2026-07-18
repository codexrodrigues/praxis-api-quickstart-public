package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtraordinaryBenefitAuthoritativeEvaluationServiceTest {

    @Test
    void commandCannotSupplyServerOwnedEligibilityFacts() {
        ExtraordinaryBenefitAuthoritativeEvaluationCommand command =
                new ExtraordinaryBenefitAuthoritativeEvaluationCommand(
                        "REQ-001",
                        ExtraordinaryBenefitReason.DISASTER_RECOVERY,
                        LocalDate.parse("2026-07-10"),
                        new BigDecimal("1200.00"),
                        "FACT-001",
                        LocalDate.parse("2026-07-20"),
                        "America/Sao_Paulo");

        ExtraordinaryBenefitEvaluationRequest request =
                ExtraordinaryBenefitAuthoritativeEvaluationService.toEvaluationRequest(command, snapshot());

        assertThat(request.workerStatus()).isEqualTo("ACTIVE");
        assertThat(request.duplicateGrant()).isFalse();
        assertThat(request.programActive()).isTrue();
        assertThat(request.programMaximumAmount()).isEqualByComparingTo("5000.00");
        assertThat(request.customerAdditionalEligible()).isTrue();
        assertThat(request.allowedPaymentDates()).containsExactly(LocalDate.parse("2026-07-20"));
        assertThat(request.availableBudgetAmount()).isEqualByComparingTo("25000.00");
    }

    private ExtraordinaryBenefitFactSnapshot snapshot() {
        return new ExtraordinaryBenefitFactSnapshot(
                new ExtraordinaryBenefitAuthoritativeFacts(
                        "ACTIVE", false, true, new BigDecimal("5000.00"), true,
                        List.of(LocalDate.parse("2026-07-20")), new BigDecimal("25000.00")),
                new RuleFactProvenance(
                        JdbcExtraordinaryBenefitFactProvider.PROVIDER_KEY,
                        "fictional-read-model",
                        "B2D7F9EA0B1B9734B1D8F04DA9E11D831F320EAAC4C713FB7D2C86169C8F9378",
                        1,
                        Instant.parse("2026-07-16T00:00:00Z"),
                        Instant.parse("2026-07-16T12:00:00Z"),
                        "E57B638474D9B462A15CDA98CA434C8A3FD63B923E7C3311DB3A8FDC402D73B4"));
    }
}
