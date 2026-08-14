package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.determination.PayrollNetSalaryDeterminationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Determinacao financeira idempotente compartilhada pelo preview reativo e pelo comando final. */
@Service
public class PayrollNetSalaryDeterminationService {

    private static final String DECISION_VERSION = "payroll-net-v1";
    private final AppliedReactiveDeterminationResolver appliedDeterminationResolver;

    public PayrollNetSalaryDeterminationService(
            AppliedReactiveDeterminationResolver appliedDeterminationResolver
    ) {
        this.appliedDeterminationResolver = appliedDeterminationResolver;
    }

    public PayrollNetSalaryDeterminationResponse determine(
            BigDecimal grossSalary,
            BigDecimal totalDiscounts
    ) {
        // An applied tenant decision selects this explicit canonical provider. Invalid
        // bindings or operation identity fail closed before any financial value is derived.
        appliedDeterminationResolver.requirePayrollSelection();
        if (grossSalary == null || totalDiscounts == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "salarioBruto and totalDescontos are required");
        }
        if (grossSalary.signum() < 0 || totalDiscounts.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Payroll values must not be negative");
        }
        if (totalDiscounts.compareTo(grossSalary) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "totalDescontos must not exceed salarioBruto");
        }
        BigDecimal netSalary = grossSalary
                .subtract(totalDiscounts)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new PayrollNetSalaryDeterminationResponse(netSalary, DECISION_VERSION);
    }

    public void validateFinalCommand(FolhasPagamentoDTO dto) {
        PayrollNetSalaryDeterminationResponse authoritative = determine(
                dto.getSalarioBruto(),
                dto.getTotalDescontos());
        if (dto.getSalarioLiquido() == null
                || authoritative.salarioLiquido().compareTo(dto.getSalarioLiquido()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "salarioLiquido does not match the authoritative payroll determination");
        }
    }
}
