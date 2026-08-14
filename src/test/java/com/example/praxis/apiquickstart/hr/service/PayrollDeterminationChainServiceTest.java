package com.example.praxis.apiquickstart.hr.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.example.praxis.apiquickstart.hr.dto.CreateFolhasPagamentoDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PayrollDeterminationChainServiceTest {

    private static final AppliedReactiveDeterminationResolver RESOLVER =
            mock(AppliedReactiveDeterminationResolver.class);
    private final PayrollDeterminationChainService service = new PayrollDeterminationChainService(
            new PayrollNetSalaryDeterminationService(RESOLVER),
            new PayrollPaymentDateDeterminationService(RESOLVER));

    @Test
    void acceptsOnlyTheFinalDraftProducedByBothAuthoritativeDeterminations() {
        CreateFolhasPagamentoDTO dto = payroll("7549.65", LocalDate.of(2026, 5, 7));

        assertThatCode(() -> service.validateFinalCommand(dto)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAValidNetSalaryWithAManipulatedDownstreamPaymentDate() {
        CreateFolhasPagamentoDTO dto = payroll("7549.65", LocalDate.of(2026, 5, 8));

        assertThatThrownBy(() -> service.validateFinalCommand(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("payroll calendar determination");
    }

    @Test
    void rejectsTheChainBeforeSchedulingWhenTheUpstreamNetSalaryWasManipulated() {
        CreateFolhasPagamentoDTO dto = payroll("9999.99", LocalDate.of(2026, 5, 7));

        assertThatThrownBy(() -> service.validateFinalCommand(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("authoritative payroll determination");
    }

    private CreateFolhasPagamentoDTO payroll(String netSalary, LocalDate paymentDate) {
        CreateFolhasPagamentoDTO dto = new CreateFolhasPagamentoDTO();
        dto.setAno(2026);
        dto.setMes(4);
        dto.setSalarioBruto(new BigDecimal("10000.00"));
        dto.setTotalDescontos(new BigDecimal("2450.35"));
        dto.setSalarioLiquido(new BigDecimal(netSalary));
        dto.setDataPagamento(paymentDate);
        return dto;
    }
}
