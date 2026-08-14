package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.example.praxis.apiquickstart.hr.dto.determination.PayrollPaymentDateDeterminationResponse;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Politica demonstrativa deterministica, sem feriados, compartilhada pelo preview e comando final. */
@Service
public class PayrollPaymentDateDeterminationService {

    private static final String DECISION_VERSION = "payroll-calendar-v1";
    private final AppliedReactiveDeterminationResolver appliedDeterminationResolver;

    public PayrollPaymentDateDeterminationService(
            AppliedReactiveDeterminationResolver appliedDeterminationResolver
    ) {
        this.appliedDeterminationResolver = appliedDeterminationResolver;
    }

    public PayrollPaymentDateDeterminationResponse determine(
            Integer year,
            Integer month,
            BigDecimal authoritativeNetSalary
    ) {
        // Config selects this exact host provider for the authenticated tenant/environment.
        // The calendar remains backend-owned and is never copied into structural metadata.
        appliedDeterminationResolver.requirePayrollPaymentDateSelection();
        if (year == null || month == null || authoritativeNetSalary == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ano, mes and salarioLiquido are required");
        }
        if (year < 1900 || year > 2100 || month < 1 || month > 12
                || authoritativeNetSalary.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Payroll period and authoritative net salary must be valid");
        }

        LocalDate paymentDate = authoritativeNetSalary.signum() == 0
                ? LocalDate.of(year, month, 1).withDayOfMonth(
                        LocalDate.of(year, month, 1).lengthOfMonth())
                : fifthWeekday(LocalDate.of(year, month, 1).plusMonths(1));
        return new PayrollPaymentDateDeterminationResponse(paymentDate, DECISION_VERSION);
    }

    private LocalDate fifthWeekday(LocalDate firstDay) {
        LocalDate cursor = firstDay;
        int businessDays = 0;
        while (true) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY
                    && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                businessDays += 1;
                if (businessDays == 5) return cursor;
            }
            cursor = cursor.plusDays(1);
        }
    }
}
