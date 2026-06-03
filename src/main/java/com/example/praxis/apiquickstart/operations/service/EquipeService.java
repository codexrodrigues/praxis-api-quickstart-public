package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.EquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.EquipeFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.mapper.EquipeMapper;
import com.example.praxis.apiquickstart.operations.repository.EquipeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
/**
 * Service de referência para cadastro de equipes operacionais.
 *
 * <p>Ele demonstra a implementação mínima de um recurso organizacional no
 * quickstart, reaproveitando a infraestrutura genérica de CRUD enquanto deixa
 * o mapeamento explícito e didático. Para quem usa o projeto como guia, este
 * service mostra onde a lógica de atualização de equipes deve ser centralizada
 * antes de aparecer em controllers, workflows ou surfaces derivadas.</p>
 */
public class EquipeService extends AbstractQuickstartCrudService<Equipe, EquipeDTO, Integer, EquipeFilterDTO, CreateEquipeDTO, UpdateEquipeDTO> {

    private final EquipeMapper mapper;

    public EquipeService(EquipeRepository repository, EquipeMapper mapper) {
        super(repository, Equipe.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Equipe::getId);
        this.mapper = mapper;
    }

    @Override
    public Equipe mergeUpdate(Equipe existing, Equipe fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}







