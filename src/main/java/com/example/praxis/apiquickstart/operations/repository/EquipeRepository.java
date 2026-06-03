package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.Equipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EquipeRepository extends BaseCrudRepository<Equipe, Integer> {

    @Override
    @EntityGraph(value = "Equipe.detail")
    Optional<Equipe> findById(Integer id);

    @Override
    @EntityGraph(value = "Equipe.detail")
    List<Equipe> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Equipe.detail")
    Page<Equipe> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Equipe.detail")
    List<Equipe> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Equipe.detail")
    Page<Equipe> findAll(Specification<Equipe> spec, Pageable pageable);
}


