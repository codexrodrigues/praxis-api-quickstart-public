package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Departamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface DepartamentoRepository extends BaseCrudRepository<Departamento, Integer> {

    @Override
    @EntityGraph(value = "Departamento.detail")
    Optional<Departamento> findById(Integer id);

    @Override
    @EntityGraph(value = "Departamento.detail")
    List<Departamento> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Departamento.detail")
    Page<Departamento> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Departamento.detail")
    List<Departamento> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Departamento.detail")
    Page<Departamento> findAll(Specification<Departamento> spec, Pageable pageable);
}
