package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.MissaoParticipante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface MissaoParticipanteRepository extends BaseCrudRepository<MissaoParticipante, Integer> {

    @Override
    @EntityGraph(value = "MissaoParticipante.detail")
    Optional<MissaoParticipante> findById(Integer id);

    @Override
    @EntityGraph(value = "MissaoParticipante.detail")
    List<MissaoParticipante> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "MissaoParticipante.detail")
    Page<MissaoParticipante> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "MissaoParticipante.detail")
    List<MissaoParticipante> findAll(Sort sort);

    @Override
    @EntityGraph(value = "MissaoParticipante.detail")
    Page<MissaoParticipante> findAll(Specification<MissaoParticipante> spec, Pageable pageable);

    @EntityGraph(value = "MissaoParticipante.detail")
    @Query("""
            select participante
              from MissaoParticipante participante
             where participante.missao.id = :missaoId
             order by participante.ordem asc, participante.id asc
            """)
    List<MissaoParticipante> findByMissaoIdForCommandCenter(@Param("missaoId") Integer missaoId);

    @EntityGraph(value = "MissaoParticipante.detail")
    @Query("""
            select participante
              from MissaoParticipante participante
             where participante.missao.id = :missaoId
             order by participante.ordem asc, participante.id asc
            """)
    List<MissaoParticipante> findByMissaoIdForPlanning(@Param("missaoId") Integer missaoId);

    @EntityGraph(value = "MissaoParticipante.detail")
    @Query("""
            select participante
              from MissaoParticipante participante
             where participante.funcionario.id = :funcionarioId
             order by participante.missao.inicioPrev desc, participante.missao.id desc, participante.ordem asc, participante.id asc
            """)
    List<MissaoParticipante> findByFuncionarioIdForEmployeeSurface(@Param("funcionarioId") Integer funcionarioId);
}


