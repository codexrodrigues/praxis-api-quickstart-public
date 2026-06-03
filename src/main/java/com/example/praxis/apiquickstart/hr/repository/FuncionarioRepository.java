package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends BaseCrudRepository<Funcionario, Integer> {

    @Override
    @EntityGraph(value = "Funcionario.detail")
    Optional<Funcionario> findById(Integer id);

    @Override
    @EntityGraph(value = "Funcionario.detail")
    List<Funcionario> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Funcionario.detail")
    Page<Funcionario> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Funcionario.detail")
    List<Funcionario> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Funcionario.detail")
    Page<Funcionario> findAll(Specification<Funcionario> spec, Pageable pageable);
}
