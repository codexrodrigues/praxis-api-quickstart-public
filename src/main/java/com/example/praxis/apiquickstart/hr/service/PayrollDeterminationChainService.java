package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Reexecuta a cadeia completa antes de qualquer create/update de folha. */
@Service
public class PayrollDeterminationChainService {

    private final PayrollNetSalaryDeterminationService netSalaryDeterminationService;
    private final PayrollPaymentDateDeterminationService paymentDateDeterminationService;

    public PayrollDeterminationChainService(
            PayrollNetSalaryDeterminationService netSalaryDeterminationService,
            PayrollPaymentDateDeterminationService paymentDateDeterminationService
    ) {
        this.netSalaryDeterminationService = netSalaryDeterminationService;
        this.paymentDateDeterminationService = paymentDateDeterminationService;
    }

    public void validateFinalCommand(FolhasPagamentoDTO dto) {
        netSalaryDeterminationService.validateFinalCommand(dto);
        var schedule = paymentDateDeterminationService.determine(
                dto.getAno(), dto.getMes(), dto.getSalarioLiquido());
        if (dto.getDataPagamento() == null
                || !schedule.dataPagamento().equals(dto.getDataPagamento())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "dataPagamento does not match the authoritative payroll calendar determination");
        }
    }
}
