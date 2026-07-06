package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.HistoricosCargo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface HistoricosCargoRepository extends BaseCrudRepository<HistoricosCargo, Integer> {

    @EntityGraph(value = "HistoricosCargo.detail")
    List<HistoricosCargo> findByFuncionarioIdOrderByDataInicioDesc(Integer funcionarioId);

    @Override
    @EntityGraph(value = "HistoricosCargo.detail")
    Optional<HistoricosCargo> findById(Integer id);

    @Override
    @EntityGraph(value = "HistoricosCargo.detail")
    List<HistoricosCargo> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "HistoricosCargo.detail")
    Page<HistoricosCargo> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "HistoricosCargo.detail")
    List<HistoricosCargo> findAll(Sort sort);

    @Override
    @EntityGraph(value = "HistoricosCargo.detail")
    Page<HistoricosCargo> findAll(Specification<HistoricosCargo> spec, Pageable pageable);
}
