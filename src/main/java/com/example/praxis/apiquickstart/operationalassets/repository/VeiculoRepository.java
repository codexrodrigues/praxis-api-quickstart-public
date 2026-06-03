package com.example.praxis.apiquickstart.operationalassets.repository;

import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends BaseCrudRepository<Veiculo, Integer> {

    @Override
    @EntityGraph(value = "Veiculo.detail")
    Optional<Veiculo> findById(Integer id);

    @Override
    @EntityGraph(value = "Veiculo.detail")
    List<Veiculo> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Veiculo.detail")
    Page<Veiculo> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Veiculo.detail")
    List<Veiculo> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Veiculo.detail")
    Page<Veiculo> findAll(Specification<Veiculo> spec, Pageable pageable);
}



