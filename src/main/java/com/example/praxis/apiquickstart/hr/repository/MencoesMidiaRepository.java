package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.MencoesMidia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface MencoesMidiaRepository extends BaseCrudRepository<MencoesMidia, Integer> {

    @Override
    @EntityGraph(value = "MencoesMidia.detail")
    Optional<MencoesMidia> findById(Integer id);

    @Override
    @EntityGraph(value = "MencoesMidia.detail")
    List<MencoesMidia> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "MencoesMidia.detail")
    Page<MencoesMidia> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "MencoesMidia.detail")
    List<MencoesMidia> findAll(Sort sort);

    @Override
    @EntityGraph(value = "MencoesMidia.detail")
    Page<MencoesMidia> findAll(Specification<MencoesMidia> spec, Pageable pageable);
}
