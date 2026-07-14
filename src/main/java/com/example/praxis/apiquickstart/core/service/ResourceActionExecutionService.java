package com.example.praxis.apiquickstart.core.service;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.repository.ResourceActionExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.praxisplatform.uischema.action.ActionScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

/** Owns request fingerprinting and reservation for idempotent collection actions. */
@Service
public class ResourceActionExecutionService {
    private static final String COLLECTION_RESOURCE_ID = "__collection__";
    private final ResourceActionExecutionRepository repository;
    private final ObjectMapper objectMapper;
    public ResourceActionExecutionService(ResourceActionExecutionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<ResourceActionExecution> findCompletedReplay(
            String resourceKey, String actionId, String idempotencyKey, Object command
    ) {
        return findCompletedReplay(resourceKey, null, actionId, idempotencyKey, command);
    }

    public Optional<ResourceActionExecution> findCompletedReplay(
            String resourceKey, Object resourceId, String actionId, String idempotencyKey, Object command
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String hash = hash(command);
        Optional<ResourceActionExecution> existing = repository
                .findByResourceKeyAndResourceIdAndActionIdAndActorSubjectAndIdempotencyKey(
                        resourceKey, normalizeResourceId(resourceId), actionId, currentActorSubject(), idempotencyKey.trim());
        if (existing.isPresent()) {
            verifyCommandHash(existing.get(), hash);
            if ("COMPLETED".equals(existing.get().getExecutionStatus())) return existing;
            throw executionUnavailable(existing.get());
        }
        return Optional.empty();
    }

    public Optional<ResourceActionExecution> reserve(
            String resourceKey, String actionId, ActionScope actionScope, String idempotencyKey,
            Object command, String correlationId, String actorSubject
    ) {
        return reserve(resourceKey, null, actionId, actionScope, idempotencyKey, command, correlationId, actorSubject);
    }

    public Optional<ResourceActionExecution> reserve(
            String resourceKey, Object resourceId, String actionId, ActionScope actionScope, String idempotencyKey,
            Object command, String correlationId, String actorSubject
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String normalizedKey = idempotencyKey.trim();
        String normalizedActor = normalizeActor(actorSubject);
        String normalizedResourceId = normalizeResourceId(resourceId);
        String hash = hash(command);
        try {
            return Optional.of(repository.saveAndFlush(new ResourceActionExecution(
                    UUID.randomUUID(), resourceKey, normalizedResourceId, actionId, actionScope.name(), normalizedKey, hash,
                    correlationId, currentHeader("X-Request-ID"), normalizedActor, currentAuthorities()
            )));
        } catch (DataIntegrityViolationException race) {
            ResourceActionExecution winner = repository
                    .findByResourceKeyAndResourceIdAndActionIdAndActorSubjectAndIdempotencyKey(
                            resourceKey, normalizedResourceId, actionId, normalizedActor, normalizedKey)
                    .orElseThrow(() -> race);
            verifyCommandHash(winner, hash);
            if ("COMPLETED".equals(winner.getExecutionStatus())) return Optional.of(winner);
            throw executionUnavailable(winner);
        }
    }
    public void complete(ResourceActionExecution execution, Object response) {
        try {
            execution.complete(objectMapper.valueToTree(response));
            repository.saveAndFlush(execution);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist idempotent workflow result", ex);
        }
    }
    public void fail(ResourceActionExecution execution, RuntimeException failure) {
        execution.fail("WORKFLOW_EXECUTION_FAILED", failure.getMessage());
        repository.saveAndFlush(execution);
    }
    private void verifyCommandHash(ResourceActionExecution execution, String hash) {
        if (!execution.getRequestHash().equals(hash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key was already used with a different command.");
        }
    }
    private ResponseStatusException executionUnavailable(ResourceActionExecution execution) {
        String state = "FAILED".equals(execution.getExecutionStatus()) ? "previously failed" : "is already in progress";
        return new ResponseStatusException(HttpStatus.CONFLICT, "The idempotent workflow execution " + state + "; use a new idempotency key after investigating the outcome.");
    }
    private String currentAuthorities() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) return null;
        return authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }
    private String currentActorSubject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return normalizeActor(authentication == null ? null : authentication.getName());
    }
    private String normalizeActor(String actorSubject) {
        return actorSubject == null || actorSubject.isBlank() ? "anonymous" : actorSubject.trim();
    }
    private String normalizeResourceId(Object resourceId) {
        return resourceId == null ? COLLECTION_RESOURCE_ID : String.valueOf(resourceId);
    }
    private String currentHeader(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader(name);
            return value == null || value.isBlank() ? null : value.trim();
        }
        return null;
    }
    private String hash(Object command) {
        try {
            ObjectMapper canonicalMapper = objectMapper.copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonicalMapper.writeValueAsBytes(command))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fingerprint workflow command", ex);
        }
    }
}
