package com.example.praxis.apiquickstart.operationalassets.repository;

import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;

public interface EquipamentoAlocacaoRepository extends BaseCrudRepository<EquipamentoAlocacao, Integer> {
    List<EquipamentoAlocacao> findByEquipamentoId(Integer equipamentoId);
}


