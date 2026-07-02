package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.hr.dto.CreateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.LegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.LegacyPayCodeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.LegacyPayCode;
import com.example.praxis.apiquickstart.hr.mapper.LegacyPayCodeMapper;
import com.example.praxis.apiquickstart.hr.repository.LegacyPayCodeRepository;
import org.praxisplatform.uischema.service.base.BaseResourceCommandService;
import org.praxisplatform.uischema.service.base.DuplicateDraftLegacyBackedResourceService;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class LegacyPayCodeService extends AbstractQuickstartCrudService<
        LegacyPayCode,
        LegacyPayCodeDTO,
        Integer,
        LegacyPayCodeFilterDTO,
        CreateLegacyPayCodeDTO,
        UpdateLegacyPayCodeDTO>
        implements DuplicateDraftLegacyBackedResourceService<
        LegacyPayCodeDTO,
        Integer,
        LegacyPayCodeFilterDTO,
        CreateLegacyPayCodeDTO,
        UpdateLegacyPayCodeDTO,
        LegacyPayCodeDTO> {

    private final LegacyPayCodeMapper mapper;
    private final LegacyPayCodeLegacyAdapter legacyAdapter;

    public LegacyPayCodeService(
            LegacyPayCodeRepository repository,
            LegacyPayCodeMapper mapper,
            LegacyPayCodeLegacyAdapter legacyAdapter
    ) {
        super(repository, LegacyPayCode.class, mapper::toDto, mapper::toEntity, mapper::toEntity, LegacyPayCode::getId);
        this.mapper = mapper;
        this.legacyAdapter = legacyAdapter;
    }

    @Override
    public LegacyPayCode mergeUpdate(LegacyPayCode existing, LegacyPayCode fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public BaseResourceCommandService.SavedResult<Integer, LegacyPayCodeDTO> create(CreateLegacyPayCodeDTO dto) {
        return legacyAdapter.create(dto);
    }

    @Override
    public LegacyPayCodeDTO update(Integer id, UpdateLegacyPayCodeDTO dto) {
        return legacyAdapter.update(id, dto);
    }

    @Override
    public void deleteById(Integer id) {
        legacyAdapter.deleteById(id);
    }

    @Override
    public void deleteAllById(Collection<Integer> ids) {
        legacyAdapter.deleteAllById(ids);
    }

    @Override
    public boolean supportsDuplicateDraft() {
        return true;
    }

    @Override
    public LegacyPayCodeDTO duplicateDraft(Integer sourceId) {
        return legacyAdapter.duplicateDraft(sourceId).body();
    }
}
