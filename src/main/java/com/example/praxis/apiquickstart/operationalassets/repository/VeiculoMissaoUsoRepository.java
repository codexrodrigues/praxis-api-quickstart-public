package com.example.praxis.apiquickstart.operationalassets.repository;

import com.example.praxis.apiquickstart.operationalassets.entity.VeiculoMissaoUso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoMissaoUsoRepository extends BaseCrudRepository<VeiculoMissaoUso, Integer> {

    @Override
    @EntityGraph(value = "VeiculoMissaoUso.detail")
    Optional<VeiculoMissaoUso> findById(Integer id);

    @Override
    @EntityGraph(value = "VeiculoMissaoUso.detail")
    List<VeiculoMissaoUso> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "VeiculoMissaoUso.detail")
    Page<VeiculoMissaoUso> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "VeiculoMissaoUso.detail")
    List<VeiculoMissaoUso> findAll(Sort sort);

    @Override
    @EntityGraph(value = "VeiculoMissaoUso.detail")
    Page<VeiculoMissaoUso> findAll(Specification<VeiculoMissaoUso> spec, Pageable pageable);
}



