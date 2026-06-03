package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoAlocacaoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoAlocacaoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoAlocacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
/**
 * Service de referência para alocação e histórico de uso de equipamentos.
 *
 * <p>No quickstart, esta classe evidencia que rastreabilidade patrimonial também
 * pode ser modelada no pipeline CRUD genérico da plataforma. O merge explícito
 * ajuda a explicar o ponto em que regras futuras de disponibilidade, devolução
 * ou auditoria podem ser incorporadas sem alterar o contrato público já exposto
 * pelo recurso de alocação.</p>
 */
public class EquipamentoAlocacaoService extends AbstractQuickstartCrudService<EquipamentoAlocacao, EquipamentoAlocacaoDTO, Integer, EquipamentoAlocacaoFilterDTO, CreateEquipamentoAlocacaoDTO, UpdateEquipamentoAlocacaoDTO> {

    private final EquipamentoAlocacaoMapper mapper;

    public EquipamentoAlocacaoService(EquipamentoAlocacaoRepository repository, EquipamentoAlocacaoMapper mapper) {
        super(repository, EquipamentoAlocacao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, EquipamentoAlocacao::getId);
        this.mapper = mapper;
    }

    @Override
    public EquipamentoAlocacao mergeUpdate(EquipamentoAlocacao existing, EquipamentoAlocacao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}







