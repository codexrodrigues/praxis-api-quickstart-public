package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.Endereco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends BaseCrudRepository<Endereco, Integer> {

    @Override
    @EntityGraph(value = "Endereco.detail")
    Optional<Endereco> findById(Integer id);

    @Override
    @EntityGraph(value = "Endereco.detail")
    List<Endereco> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Endereco.detail")
    Page<Endereco> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Endereco.detail")
    List<Endereco> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Endereco.detail")
    Page<Endereco> findAll(Specification<Endereco> spec, Pageable pageable);
}
