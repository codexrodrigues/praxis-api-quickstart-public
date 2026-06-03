package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.EventosFolha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EventosFolhaRepository extends BaseCrudRepository<EventosFolha, Integer> {

    @Override
    @EntityGraph(value = "EventosFolha.detail")
    Optional<EventosFolha> findById(Integer id);

    @Override
    @EntityGraph(value = "EventosFolha.detail")
    List<EventosFolha> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "EventosFolha.detail")
    Page<EventosFolha> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "EventosFolha.detail")
    List<EventosFolha> findAll(Sort sort);

    @Override
    @EntityGraph(value = "EventosFolha.detail")
    Page<EventosFolha> findAll(Specification<EventosFolha> spec, Pageable pageable);
}
