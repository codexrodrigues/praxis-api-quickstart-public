package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Indenizacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface IndenizacaoRepository extends BaseCrudRepository<Indenizacao, Integer> {

    @Override
    @EntityGraph(value = "Indenizacao.detail")
    Optional<Indenizacao> findById(Integer id);

    @Override
    @EntityGraph(value = "Indenizacao.detail")
    List<Indenizacao> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Indenizacao.detail")
    Page<Indenizacao> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Indenizacao.detail")
    List<Indenizacao> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Indenizacao.detail")
    Page<Indenizacao> findAll(Specification<Indenizacao> spec, Pageable pageable);
}
