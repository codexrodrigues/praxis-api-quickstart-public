package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.FuncionarioLotacaoDepartamento;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read boundary for effective-dated department ownership. */
public interface FuncionarioLotacaoDepartamentoRepository extends JpaRepository<FuncionarioLotacaoDepartamento, Long> {

    @Query("""
            select assignment
            from FuncionarioLotacaoDepartamento assignment
            join fetch assignment.departamento
            where assignment.funcionario.id = :funcionarioId
              and assignment.effectiveFrom <= :effectiveDate
              and (assignment.effectiveTo is null or assignment.effectiveTo > :effectiveDate)
            """)
    Optional<FuncionarioLotacaoDepartamento> findEffectiveAssignment(
            @Param("funcionarioId") Integer funcionarioId,
            @Param("effectiveDate") LocalDate effectiveDate);
}
