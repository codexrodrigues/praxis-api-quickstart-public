package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.EquipeMembro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EquipeMembroRepository extends BaseCrudRepository<EquipeMembro, Integer> {

    @EntityGraph(value = "EquipeMembro.detail")
    @Query("""
            select membro
              from EquipeMembro membro
             where membro.equipe.id = :equipeId
             order by case when membro.dataSaida is null then 0 else 1 end,
                      membro.dataEntrada desc,
                      membro.id asc
            """)
    List<EquipeMembro> findByEquipeIdForTeamSurface(@Param("equipeId") Integer equipeId);

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
