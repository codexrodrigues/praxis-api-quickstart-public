package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.LicencasOperacao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface LicencasOperacaoRepository extends BaseCrudRepository<LicencasOperacao, Integer> {

    interface LicencaValiditySnapshot {
        java.time.LocalDate getValidoDe();
        java.time.LocalDate getValidoAte();
    }

    @Override
    @EntityGraph(value = "LicencasOperacao.detail")
    Optional<LicencasOperacao> findById(Integer id);

    @Override
    @EntityGraph(value = "LicencasOperacao.detail")
    List<LicencasOperacao> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "LicencasOperacao.detail")
    Page<LicencasOperacao> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "LicencasOperacao.detail")
    List<LicencasOperacao> findAll(Sort sort);

    @Override
    @EntityGraph(value = "LicencasOperacao.detail")
    Page<LicencasOperacao> findAll(Specification<LicencasOperacao> spec, Pageable pageable);

    @Query("""
            select l.validoDe as validoDe, l.validoAte as validoAte
              from LicencasOperacao l
             where l.id = :id
            """)
    Optional<LicencaValiditySnapshot> findValiditySnapshotById(@Param("id") Integer id);
}


