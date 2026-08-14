package com.example.praxis.apiquickstart.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PayrollPaymentDateDeterminationServiceTest {

    private final AppliedReactiveDeterminationResolver resolver =
            mock(AppliedReactiveDeterminationResolver.class);
    private final PayrollPaymentDateDeterminationService service =
            new PayrollPaymentDateDeterminationService(resolver);

    @Test
    void determinesTheFifthBusinessDayAfterThePayrollPeriod() {
        var result = service.determine(2026, 4, new BigDecimal("7549.65"));

        assertThat(result.dataPagamento()).isEqualTo(LocalDate.of(2026, 5, 7));
        assertThat(result.decisionVersion()).isEqualTo("payroll-calendar-v1");
        verify(resolver).requirePayrollPaymentDateSelection();
    }

    @Test
    void keepsZeroNetPayrollInsideItsCompetenceWithoutBankSettlement() {
        var result = service.determine(2026, 2, BigDecimal.ZERO);

        assertThat(result.dataPagamento()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void rejectsAnInvalidAuthoritativeNetSalary() {
        assertThatThrownBy(() -> service.determine(2026, 4, new BigDecimal("-0.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must be valid");
    }
}
