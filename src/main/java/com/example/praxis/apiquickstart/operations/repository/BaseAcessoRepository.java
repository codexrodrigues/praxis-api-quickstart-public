package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.BaseAcesso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BaseAcessoRepository extends BaseCrudRepository<BaseAcesso, Integer> {

    @Override
    @EntityGraph(value = "BaseAcesso.detail")
    Optional<BaseAcesso> findById(Integer id);

    @Override
    @EntityGraph(value = "BaseAcesso.detail")
    List<BaseAcesso> findAllById(Iterable<Integer> ids);

    @Override
    @EntityGraph(value = "BaseAcesso.detail")
    Page<BaseAcesso> findAll(Pageable pageable);

    @Override
    @EntityGraph(value = "BaseAcesso.detail")
    List<BaseAcesso> findAll(Sort sort);

    @Override
    @EntityGraph(value = "BaseAcesso.detail")
    Page<BaseAcesso> findAll(Specification<BaseAcesso> spec, Pageable pageable);

    @Query("select b.ativo from BaseAcesso b where b.id = :id")
    Optional<Boolean> findAtivoById(@Param("id") Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BaseAcesso b
               set b.ativo = :targetAtivo
             where b.id = :id
               and b.ativo = :expectedAtivo
            """)
    int transitionAtivo(
            @Param("id") Integer id,
            @Param("expectedAtivo") boolean expectedAtivo,
            @Param("targetAtivo") boolean targetAtivo
    );
}


