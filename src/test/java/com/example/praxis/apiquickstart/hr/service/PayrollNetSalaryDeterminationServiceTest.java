package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.example.praxis.apiquickstart.hr.dto.CreateFolhasPagamentoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PayrollNetSalaryDeterminationServiceTest {

    private final AppliedReactiveDeterminationResolver resolver =
            mock(AppliedReactiveDeterminationResolver.class);
    private final PayrollNetSalaryDeterminationService service =
            new PayrollNetSalaryDeterminationService(resolver);

    @Test
    void determinesNetSalaryFromMultipleFinancialInputs() {
        var result = service.determine(
                new BigDecimal("10000.00"),
                new BigDecimal("2450.35"));

        assertThat(result.salarioLiquido()).isEqualByComparingTo("7549.65");
        assertThat(result.decisionVersion()).isEqualTo("payroll-net-v1");
    }

    @Test
    void rejectsDiscountsAboveGrossSalary() {
        assertThatThrownBy(() -> service.determine(
                new BigDecimal("1000.00"),
                new BigDecimal("1000.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void finalCommandRejectsManipulatedDerivedValue() {
        CreateFolhasPagamentoDTO dto = new CreateFolhasPagamentoDTO();
        dto.setSalarioBruto(new BigDecimal("10000.00"));
        dto.setTotalDescontos(new BigDecimal("2450.35"));
        dto.setSalarioLiquido(new BigDecimal("9999.99"));

        assertThatThrownBy(() -> service.validateFinalCommand(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("authoritative payroll determination");
    }

    @Test
    void previewAndFinalCommandBothResolveTheAppliedConfigSelection() {
        AppliedReactiveDeterminationResolver resolver =
                mock(AppliedReactiveDeterminationResolver.class);
        PayrollNetSalaryDeterminationService governedService =
                new PayrollNetSalaryDeterminationService(resolver);

        governedService.determine(new BigDecimal("10000.00"), new BigDecimal("2450.35"));
        CreateFolhasPagamentoDTO dto = new CreateFolhasPagamentoDTO();
        dto.setSalarioBruto(new BigDecimal("10000.00"));
        dto.setTotalDescontos(new BigDecimal("2450.35"));
        dto.setSalarioLiquido(new BigDecimal("7549.65"));
        governedService.validateFinalCommand(dto);

        verify(resolver, times(2)).requirePayrollSelection();
    }
}
