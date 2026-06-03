package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FolhasPagamentoRepository extends BaseCrudRepository<FolhasPagamento, Integer> {

    @Query("select fp.dataPagamento from FolhasPagamento fp where fp.id = :id")
    Optional<LocalDate> findPaymentDateById(@Param("id") Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FolhasPagamento fp
               set fp.dataPagamento = :paidAt
             where fp.id = :id
               and fp.dataPagamento > :paidAt
            """)
    int markAsPaid(@Param("id") Integer id, @Param("paidAt") LocalDate paidAt);
}
