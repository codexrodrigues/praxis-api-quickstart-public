package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.EquipeMembro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EquipeMembroRepository extends BaseCrudRepository<EquipeMembro, Integer> {

    @Override
    @EntityGraph(value = "EquipeMembro.detail")
    Optional<EquipeMembro> findById(Integer id);

    @Override
    @EntityGraph(value = "EquipeMembro.detail")
    List<EquipeMembro> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "EquipeMembro.detail")
    Page<EquipeMembro> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "EquipeMembro.detail")
    List<EquipeMembro> findAll(Sort sort);

    @Override
    @EntityGraph(value = "EquipeMembro.detail")
    Page<EquipeMembro> findAll(Specification<EquipeMembro> spec, Pageable pageable);
}


