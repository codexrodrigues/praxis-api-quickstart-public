package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Reputacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface ReputacaoRepository extends BaseCrudRepository<Reputacao, Integer> {

    @Override
    @EntityGraph(value = "Reputacao.detail")
    Optional<Reputacao> findById(Integer id);

    @Override
    @EntityGraph(value = "Reputacao.detail")
    List<Reputacao> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Reputacao.detail")
    Page<Reputacao> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Reputacao.detail")
    List<Reputacao> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Reputacao.detail")
    Page<Reputacao> findAll(Specification<Reputacao> spec, Pageable pageable);
}
