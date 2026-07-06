package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.HistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HistoricosCargoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.HistoricosCargo;
import com.example.praxis.apiquickstart.hr.mapper.HistoricosCargoMapper;
import com.example.praxis.apiquickstart.hr.repository.HistoricosCargoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoricosCargoService extends AbstractQuickstartCrudService<HistoricosCargo, HistoricosCargoDTO, Integer, HistoricosCargoFilterDTO, CreateHistoricosCargoDTO, UpdateHistoricosCargoDTO> {

    private final HistoricosCargoMapper mapper;
    private final HistoricosCargoRepository repository;

    public HistoricosCargoService(HistoricosCargoRepository repository, HistoricosCargoMapper mapper) {
        super(repository, HistoricosCargo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, HistoricosCargo::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public HistoricosCargo mergeUpdate(HistoricosCargo existing, HistoricosCargo fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    public List<HistoricosCargoDTO> findByFuncionarioIdForEmployeeSurface(Integer funcionarioId) {
        return repository.findByFuncionarioIdOrderByDataInicioDesc(funcionarioId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}


