package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.IdentidadeSecreta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface IdentidadeSecretaRepository extends BaseCrudRepository<IdentidadeSecreta, Integer> {

    @Override
    @EntityGraph(value = "IdentidadeSecreta.detail")
    Optional<IdentidadeSecreta> findById(Integer id);

    @Override
    @EntityGraph(value = "IdentidadeSecreta.detail")
    List<IdentidadeSecreta> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "IdentidadeSecreta.detail")
    Page<IdentidadeSecreta> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "IdentidadeSecreta.detail")
    List<IdentidadeSecreta> findAll(Sort sort);

    @Override
    @EntityGraph(value = "IdentidadeSecreta.detail")
    Page<IdentidadeSecreta> findAll(Specification<IdentidadeSecreta> spec, Pageable pageable);
}
