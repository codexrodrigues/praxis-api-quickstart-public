package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.entity.FuncionarioLotacaoDepartamento;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioLotacaoDepartamentoRepository;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the only department that may be used to attribute a dated HR fact. */
@Service
public class FuncionarioLotacaoDepartamentoResolver {

    private final FuncionarioLotacaoDepartamentoRepository repository;

    public FuncionarioLotacaoDepartamentoResolver(FuncionarioLotacaoDepartamentoRepository repository) {
        this.repository = repository;
    }

    public FuncionarioLotacaoDepartamento resolveRequired(Integer funcionarioId, LocalDate effectiveDate) {
        if (funcionarioId == null || effectiveDate == null) {
            throw new IllegalArgumentException("Employee identity and effective date are required.");
        }
        return repository.findEffectiveAssignment(funcionarioId, effectiveDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No effective department assignment exists for the employee on the requested date."));
    }
}
