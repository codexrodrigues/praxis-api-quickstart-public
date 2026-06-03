package com.example.praxis.apiquickstart.operationalassets.repository;

import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EquipamentoRepository extends BaseCrudRepository<Equipamento, Integer> {

    @Override
    @EntityGraph(value = "Equipamento.detail")
    Optional<Equipamento> findById(Integer id);

    @Override
    @EntityGraph(value = "Equipamento.detail")
    List<Equipamento> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "Equipamento.detail")
    Page<Equipamento> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "Equipamento.detail")
    List<Equipamento> findAll(Sort sort);

    @Override
    @EntityGraph(value = "Equipamento.detail")
    Page<Equipamento> findAll(Specification<Equipamento> spec, Pageable pageable);
}



