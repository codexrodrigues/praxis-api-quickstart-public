package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.BaseDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseOpsContextDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.BaseFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Base;
import com.example.praxis.apiquickstart.operations.mapper.BaseMapper;
import com.example.praxis.apiquickstart.operations.repository.BaseRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de bases operacionais com foco em contexto tatico do ativo.
 *
 * <p>Ele evidencia a diferenca entre update completo do cadastro e ajuste parcial do contexto
 * operacional publicado pela surface {@code ops-context}.</p>
 */
@Service
public class BaseService extends AbstractQuickstartCrudService<Base, BaseDTO, Integer, BaseFilterDTO, CreateBaseDTO, UpdateBaseDTO> {

    private final BaseRepository repository;
    private final BaseMapper mapper;

    public BaseService(BaseRepository repository, BaseMapper mapper) {
        super(repository, Base.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Base::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Base mergeUpdate(Base existing, Base fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public BaseDTO updateOpsContext(Integer id, UpdateBaseOpsContextDTO dto) {
        Base existing = repository.findById(id).orElseThrow(this::getNotFoundException);
        existing.setSigilo(dto.getSigilo());
        existing.setPlaneta(dto.getPlaneta());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        Base saved = refreshManaged(repository.save(existing));
        return mapper.toDto(saved);
    }

    /** Recarrega a entidade apos salvar para devolver o estado persistido mais recente. */
    private Base refreshManaged(Base entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        Base managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }
}







