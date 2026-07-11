package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.entity.EventosFolha;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;
import com.example.praxis.apiquickstart.hr.repository.EventosFolhaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Executes one bulk-approval item in an isolated transaction so partial results are durable. */
@Service
public class EventosFolhaApprovalItemService {
    private final EventosFolhaRepository repository;
    private final ResourceActionTransitionService transitionService;

    public EventosFolhaApprovalItemService(EventosFolhaRepository repository, ResourceActionTransitionService transitionService) {
        this.repository = repository;
        this.transitionService = transitionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID approve(Integer id, BulkApproveEventosFolhaRequestDTO command, String actorSubject, String correlationId) {
        var replay = transitionService.findReplay("human-resources.eventos-folha", id, "bulk-approve");
        if (replay.isPresent()) return replay.get();
        EventosFolha event = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll event not found."));
        if (event.getStatus() != StatusEventoFolha.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + event.getStatus());
        }
        long versionBefore = event.getVersion() == null ? 0L : event.getVersion();
        event.setStatus(StatusEventoFolha.APROVADO);
        EventosFolha saved = repository.saveAndFlush(event);
        return transitionService.record("human-resources.eventos-folha", id, "bulk-approve", "COLLECTION",
                StatusEventoFolha.PENDENTE.name(), StatusEventoFolha.APROVADO.name(), command.getReasonCode(),
                command.getComment(), command.getEffectiveAt(), actorSubject, correlationId, versionBefore, saved.getVersion());
    }
}
