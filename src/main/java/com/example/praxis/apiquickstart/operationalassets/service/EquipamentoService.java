package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
/**
 * Service de referência para cadastro de equipamentos.
 *
 * <p>Esta implementação mostra o caminho básico para persistir ativos no host
 * operacional de referência da Praxis: repositório, mapeamento explícito e merge
 * controlado de atualização. O valor didático aqui é mostrar que o recurso de
 * inventário reaproveita a infraestrutura comum do quickstart sem perder clareza
 * sobre onde regras de domínio adicionais deveriam evoluir.</p>
 */
public class EquipamentoService extends AbstractQuickstartCrudService<Equipamento, EquipamentoDTO, Integer, EquipamentoFilterDTO, CreateEquipamentoDTO, UpdateEquipamentoDTO> {

    private final EquipamentoMapper mapper;

    public EquipamentoService(EquipamentoRepository repository, EquipamentoMapper mapper) {
        super(repository, Equipamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Equipamento::getId);
        this.mapper = mapper;
    }

    @Override
    public Equipamento mergeUpdate(Equipamento existing, Equipamento fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}







