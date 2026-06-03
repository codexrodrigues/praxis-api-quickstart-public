package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.BaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.BaseAcessoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.BaseAcessoWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.BaseAcessoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.BaseAcesso;
import com.example.praxis.apiquickstart.operations.mapper.BaseAcessoMapper;
import com.example.praxis.apiquickstart.operations.repository.BaseAcessoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service de acesso a bases com foco em governanca de autorizacao.
 *
 * <p>Ele deixa explicita a diferenca entre revisar o nivel de acesso do vinculo existente e
 * ativar/desativar o acesso como transicao operacional de estado. Esse contraste reaparece com
 * frequencia em recursos reais da plataforma.</p>
 */
@Service
public class BaseAcessoService extends AbstractQuickstartCrudService<BaseAcesso, BaseAcessoDTO, Integer, BaseAcessoFilterDTO, CreateBaseAcessoDTO, UpdateBaseAcessoDTO> {

    private final BaseAcessoRepository repository;
    private final BaseAcessoMapper mapper;

    public BaseAcessoService(BaseAcessoRepository repository, BaseAcessoMapper mapper) {
        super(repository, BaseAcesso.class, mapper::toDto, mapper::toEntity, mapper::toEntity, BaseAcesso::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BaseAcesso mergeUpdate(BaseAcesso existing, BaseAcesso fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }

    @Transactional
    public BaseAcessoDTO reviewAccess(Integer id, ReviewBaseAcessoDTO dto) {
        BaseAcesso existing = repository.findById(id).orElseThrow(this::getNotFoundException);
        existing.setNivelAcesso(dto.getNivelAcesso());
        BaseAcesso saved = refreshManaged(repository.save(existing));
        return mapper.toDto(saved);
    }

    @Transactional
    public BaseAcessoWorkflowResultDTO activate(Integer id, BaseAcessoWorkflowRequestDTO dto) {
        return transitionAtivo(id, false, true, dto, "Access activated");
    }

    @Transactional
    public BaseAcessoWorkflowResultDTO deactivate(Integer id, BaseAcessoWorkflowRequestDTO dto) {
        return transitionAtivo(id, true, false, dto, "Access deactivated");
    }

    private BaseAcessoWorkflowResultDTO transitionAtivo(
            Integer id,
            boolean expectedAtivo,
            boolean targetAtivo,
            BaseAcessoWorkflowRequestDTO dto,
            String message
    ) {
        int updated = repository.transitionAtivo(id, expectedAtivo, targetAtivo);
        if (updated == 0) {
            Boolean currentState = repository.findAtivoById(id).orElseThrow(this::getNotFoundException);
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + (Boolean.TRUE.equals(currentState) ? "ATIVO" : "INATIVO"));
        }
        BaseAcesso entity = repository.findById(id).orElseThrow(this::getNotFoundException);
        BaseAcessoWorkflowResultDTO result = new BaseAcessoWorkflowResultDTO();
        result.setId(id);
        result.setAtivoAnterior(expectedAtivo);
        result.setAtivoAtual(targetAtivo);
        result.setNivelAcesso(entity.getNivelAcesso());
        result.setJustificativa(dto.getJustificativa());
        result.setMensagem(message);
        return result;
    }

    private BaseAcesso refreshManaged(BaseAcesso entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        BaseAcesso managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }
}






