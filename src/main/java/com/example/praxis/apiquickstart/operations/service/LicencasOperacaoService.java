package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.LicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.RenewLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.LicencasOperacaoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.LicencasOperacao;
import com.example.praxis.apiquickstart.operations.mapper.LicencasOperacaoMapper;
import com.example.praxis.apiquickstart.operations.repository.LicencasOperacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service de licencas operacionais focado em semantica de vigencia.
 *
 * <p>Este service mostra um caso em que a disponibilidade da surface depende de um estado derivado
 * de datas, nao de um enum persistido diretamente. O quickstart usa essa implementacao para
 * demonstrar como o host pode expor semantica temporal canonica sem empurrar o calculo para o
 * consumidor.</p>
 */
@Service
public class LicencasOperacaoService extends AbstractQuickstartCrudService<LicencasOperacao, LicencasOperacaoDTO, Integer, LicencasOperacaoFilterDTO, CreateLicencasOperacaoDTO, UpdateLicencasOperacaoDTO> {

    private final LicencasOperacaoRepository repository;
    private final LicencasOperacaoMapper mapper;

    public LicencasOperacaoService(LicencasOperacaoRepository repository, LicencasOperacaoMapper mapper) {
        super(repository, LicencasOperacao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, LicencasOperacao::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public LicencasOperacao mergeUpdate(LicencasOperacao existing, LicencasOperacao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public LicencasOperacaoDTO renew(Integer id, RenewLicencasOperacaoDTO dto) {
        if (dto.getValidoAte() != null && dto.getValidoAte().isBefore(dto.getValidoDe())) {
            throw new ResponseStatusException(BAD_REQUEST, "validoAte must be greater than or equal to validoDe");
        }

        LicencasOperacao existing = repository.findById(id).orElseThrow(this::getNotFoundException);
        String state = resolveState(existing, LocalDate.now());
        if (!"ATIVA".equals(state) && !"A_EXPIRAR".equals(state) && !"EXPIRADA".equals(state)) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + state);
        }

        existing.setNivel(dto.getNivel());
        existing.setValidoDe(dto.getValidoDe());
        existing.setValidoAte(dto.getValidoAte());
        LicencasOperacao saved = refreshManaged(repository.save(existing));
        return mapper.toDto(saved);
    }

    /** Traduz a janela de vigencia em um estado semantico consumido pela UI e pelo discovery. */
    private String resolveState(LicencasOperacao entity, LocalDate today) {
        if (entity.getValidoDe() != null && entity.getValidoDe().isAfter(today)) {
            return "FUTURA";
        }
        if (entity.getValidoAte() != null && entity.getValidoAte().isBefore(today)) {
            return "EXPIRADA";
        }
        if (entity.getValidoAte() != null && !entity.getValidoAte().isAfter(today.plusDays(14))) {
            return "A_EXPIRAR";
        }
        return "ATIVA";
    }

    private LicencasOperacao refreshManaged(LicencasOperacao entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        LicencasOperacao managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }
}







