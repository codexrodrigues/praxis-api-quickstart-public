package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.FuncionarioHabilidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioHabilidadeRepository extends BaseCrudRepository<FuncionarioHabilidade, Integer> {

    @Override
    @EntityGraph(value = "FuncionarioHabilidade.detail")
    Optional<FuncionarioHabilidade> findById(Integer id);

    @Override
    @EntityGraph(value = "FuncionarioHabilidade.detail")
    List<FuncionarioHabilidade> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "FuncionarioHabilidade.detail")
    Page<FuncionarioHabilidade> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "FuncionarioHabilidade.detail")
    List<FuncionarioHabilidade> findAll(Sort sort);

    @Override
    @EntityGraph(value = "FuncionarioHabilidade.detail")
    Page<FuncionarioHabilidade> findAll(Specification<FuncionarioHabilidade> spec, Pageable pageable);
}
