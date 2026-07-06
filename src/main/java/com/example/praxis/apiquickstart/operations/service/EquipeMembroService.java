package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.EquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateEquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateEquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.EquipeMembroFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.EquipeMembro;
import com.example.praxis.apiquickstart.operations.mapper.EquipeMembroMapper;
import com.example.praxis.apiquickstart.operations.repository.EquipeMembroRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * Service de referência para composição de equipes.
 *
 * <p>Esta classe reforça o uso do quickstart como material didático para recursos
 * relacionais: o vínculo entre equipe e membro continua usando o pipeline CRUD
 * comum, mas pode publicar uma versão resumida de dataset para facilitar cache
 * e invalidação em consumers. Assim, o exemplo mostra uma extensão operacional
 * simples sem abandonar o contrato metadata-driven da plataforma.</p>
 */
public class EquipeMembroService extends AbstractQuickstartCrudService<EquipeMembro, EquipeMembroDTO, Integer, EquipeMembroFilterDTO, CreateEquipeMembroDTO, UpdateEquipeMembroDTO> {

    private final EquipeMembroMapper mapper;

    public EquipeMembroService(EquipeMembroRepository repository, EquipeMembroMapper mapper) {
        super(repository, EquipeMembro.class, mapper::toDto, mapper::toEntity, mapper::toEntity, EquipeMembro::getId);
        this.mapper = mapper;
    }

    @Override
    public EquipeMembro mergeUpdate(EquipeMembro existing, EquipeMembro fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }

    public List<EquipeMembroDTO> findByEquipeIdForTeamSurface(Integer equipeId) {
        return ((EquipeMembroRepository) getRepository()).findByEquipeIdForTeamSurface(equipeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}





