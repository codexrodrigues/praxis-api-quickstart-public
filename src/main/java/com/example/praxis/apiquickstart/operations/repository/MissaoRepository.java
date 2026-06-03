package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MissaoRepository extends BaseCrudRepository<Missao, Integer> {

    @Override
    @EntityGraph(value = "Missao.detail")
    Optional<Missao> findById(Integer id);

    @Override
    @EntityGraph(value = "Missao.detail")
    List<Missao> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Missao.detail")
    Page<Missao> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Missao.detail")
    List<Missao> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Missao.detail")
    Page<Missao> findAll(Specification<Missao> spec, Pageable pageable);

    @Query("select m.status from Missao m where m.id = :id")
    Optional<MissaoStatus> findStatusById(@Param("id") Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Missao m
               set m.status = :targetStatus
             where m.id = :id
               and m.status = :expectedStatus
            """)
    int transitionStatus(
            @Param("id") Integer id,
            @Param("expectedStatus") MissaoStatus expectedStatus,
            @Param("targetStatus") MissaoStatus targetStatus
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Missao m
               set m.status = :targetStatus,
                   m.inicioReal = coalesce(m.inicioReal, :ocorridoEm)
             where m.id = :id
               and m.status = :expectedStatus
            """)
    int startMission(
            @Param("id") Integer id,
            @Param("expectedStatus") MissaoStatus expectedStatus,
            @Param("targetStatus") MissaoStatus targetStatus,
            @Param("ocorridoEm") OffsetDateTime ocorridoEm
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Missao m
               set m.status = :targetStatus,
                   m.fimReal = :ocorridoEm
             where m.id = :id
               and m.status = :expectedStatus
            """)
    int finishMission(
            @Param("id") Integer id,
            @Param("expectedStatus") MissaoStatus expectedStatus,
            @Param("targetStatus") MissaoStatus targetStatus,
            @Param("ocorridoEm") OffsetDateTime ocorridoEm
    );
}


