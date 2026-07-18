package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.entity.FuncionarioLotacaoDepartamento;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioLotacaoDepartamentoRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioLotacaoDepartamentoResolverTest {

    @Mock
    private FuncionarioLotacaoDepartamentoRepository repository;

    @Test
    void resolvesOnlyTheAssignmentEffectiveOnTheFactDate() {
        LocalDate factDate = LocalDate.of(2026, 7, 1);
        FuncionarioLotacaoDepartamento assignment = new FuncionarioLotacaoDepartamento();
        when(repository.findEffectiveAssignment(7, factDate)).thenReturn(Optional.of(assignment));

        FuncionarioLotacaoDepartamento resolved = new FuncionarioLotacaoDepartamentoResolver(repository)
                .resolveRequired(7, factDate);

        assertSame(assignment, resolved);
        verify(repository).findEffectiveAssignment(7, factDate);
    }

    @Test
    void refusesToAttributeAFactWhenHistoricalAssignmentIsMissing() {
        LocalDate factDate = LocalDate.of(2026, 7, 1);
        when(repository.findEffectiveAssignment(7, factDate)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new FuncionarioLotacaoDepartamentoResolver(repository).resolveRequired(7, factDate));

        assertEquals(422, error.getStatusCode().value());
    }
}
