package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.riskintelligence.dto.AmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.CreateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.UpdateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.AmeacaFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.riskintelligence.mapper.AmeacaMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.AmeacaRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

/**
 * Service de ameacas usado como exemplo simples de cadastro transacional no dominio de risco.
 *
 * <p>O quickstart mantem este service enxuto para reforcar que a camada de risco tambem pode usar
 * o pipeline canonico da plataforma sem complexidade extra quando nao ha workflow ou surface
 * especializada.</p>
 */
@Service
public class AmeacaService extends AbstractQuickstartCrudService<Ameaca, AmeacaDTO, Integer, AmeacaFilterDTO, CreateAmeacaDTO, UpdateAmeacaDTO> {

    private final AmeacaMapper mapper;

    public AmeacaService(AmeacaRepository repository, AmeacaMapper mapper) {
        super(repository, Ameaca.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Ameaca::getId);
        this.mapper = mapper;
    }

    @Override
    public Ameaca mergeUpdate(Ameaca existing, Ameaca fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}





