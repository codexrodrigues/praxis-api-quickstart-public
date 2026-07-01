package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.CreateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.LegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.entity.LegacyPayCode;
import com.example.praxis.apiquickstart.hr.mapper.LegacyPayCodeMapper;
import com.example.praxis.apiquickstart.hr.repository.LegacyPayCodeRepository;
import org.praxisplatform.uischema.service.base.BaseResourceCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@Component
public class LegacyPayCodeLegacyAdapter {

    private final LegacyPayCodeRepository repository;
    private final LegacyPayCodeMapper mapper;

    public LegacyPayCodeLegacyAdapter(LegacyPayCodeRepository repository, LegacyPayCodeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public BaseResourceCommandService.SavedResult<Integer, LegacyPayCodeDTO> create(CreateLegacyPayCodeDTO dto) {
        LegacyPayCode entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setStatus(normalizeStatus(entity.getStatus(), "ACTIVE"));
        entity.setActive(entity.getActive() == null ? Boolean.TRUE : entity.getActive());
        LegacyPayCode saved = repository.save(entity);
        return savedResult(saved);
    }

    @Transactional
    public LegacyPayCodeDTO update(Integer id, UpdateLegacyPayCodeDTO dto) {
        LegacyPayCode existing = findExisting(id);
        LegacyPayCode fromPayload = mapper.toEntity(dto);
        mapper.updateEntity(fromPayload, existing);
        existing.setId(id);
        existing.setStatus(normalizeStatus(existing.getStatus(), "ACTIVE"));
        existing.setActive(existing.getActive() == null ? Boolean.TRUE : existing.getActive());
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void deleteById(Integer id) {
        repository.findById(id).ifPresent(repository::delete);
    }

    @Transactional
    public void deleteAllById(Collection<Integer> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        ids.forEach(this::deleteById);
    }

    @Transactional
    public BaseResourceCommandService.SavedResult<Integer, LegacyPayCodeDTO> duplicateDraft(Integer sourceId) {
        LegacyPayCode source = findExisting(sourceId);
        LegacyPayCode draft = new LegacyPayCode();
        draft.setCode(draftCode(source.getCode()));
        draft.setDescription(source.getDescription() + " (draft)");
        draft.setPayrollCategory(source.getPayrollCategory());
        draft.setStatus("DRAFT");
        draft.setActive(Boolean.FALSE);
        return savedResult(repository.save(draft));
    }

    private LegacyPayCode findExisting(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legacy pay code not found."));
    }

    private BaseResourceCommandService.SavedResult<Integer, LegacyPayCodeDTO> savedResult(LegacyPayCode entity) {
        return new BaseResourceCommandService.SavedResult<>(entity.getId(), mapper.toDto(entity));
    }

    private String normalizeStatus(String status, String fallback) {
        return status == null || status.isBlank() ? fallback : status.trim().toUpperCase();
    }

    private String draftCode(String sourceCode) {
        String base = sourceCode == null || sourceCode.isBlank() ? "LEGACY" : sourceCode.trim();
        String candidate = base + "-DRAFT";
        return candidate.length() <= 40 ? candidate : candidate.substring(0, 40);
    }
}
