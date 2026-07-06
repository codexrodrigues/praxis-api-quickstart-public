package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Dependente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface DependenteRepository extends BaseCrudRepository<Dependente, Integer> {

    @EntityGraph(value = "Dependente.detail")
    List<Dependente> findByFuncionarioIdOrderByDataNascimentoAsc(Integer funcionarioId);

    @Override
    @EntityGraph(value = "Dependente.detail")
    Optional<Dependente> findById(Integer id);

    @Override
    @EntityGraph(value = "Dependente.detail")
    List<Dependente> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Dependente.detail")
    Page<Dependente> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Dependente.detail")
    List<Dependente> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Dependente.detail")
    Page<Dependente> findAll(Specification<Dependente> spec, Pageable pageable);
}
