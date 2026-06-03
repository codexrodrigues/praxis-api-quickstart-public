package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.MissaoEvento;
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

public interface MissaoEventoRepository extends BaseCrudRepository<MissaoEvento, Integer> {

    @Override
    @EntityGraph(value = "MissaoEvento.detail")
    Optional<MissaoEvento> findById(Integer id);

    @Override
    @EntityGraph(value = "MissaoEvento.detail")
    List<MissaoEvento> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "MissaoEvento.detail")
    Page<MissaoEvento> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "MissaoEvento.detail")
    List<MissaoEvento> findAll(Sort sort);

    @Override
    @EntityGraph(value = "MissaoEvento.detail")
    Page<MissaoEvento> findAll(Specification<MissaoEvento> spec, Pageable pageable);

    @EntityGraph(value = "MissaoEvento.detail")
    @Query("""
            select evento
              from MissaoEvento evento
             where evento.missao.id = :missaoId
             order by evento.ocorridoEm desc, evento.id desc
            """)
    List<MissaoEvento> findTop20ByMissaoIdForTimeline(@Param("missaoId") Integer missaoId);
}


