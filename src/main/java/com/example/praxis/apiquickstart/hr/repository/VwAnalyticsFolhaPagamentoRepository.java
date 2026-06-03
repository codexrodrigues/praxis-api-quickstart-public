package com.example.praxis.apiquickstart.hr.repository;

import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;

public interface VwAnalyticsFolhaPagamentoRepository extends BaseCrudRepository<VwAnalyticsFolhaPagamento, Integer> {

    List<VwAnalyticsFolhaPagamento> findTop12ByFuncionarioIdOrderByCompetenciaDesc(Integer funcionarioId);
}
