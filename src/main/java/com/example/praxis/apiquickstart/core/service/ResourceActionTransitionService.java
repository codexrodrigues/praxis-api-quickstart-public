package com.example.praxis.apiquickstart.core.service;

import com.example.praxis.apiquickstart.core.entity.ResourceActionTransition;
import com.example.praxis.apiquickstart.core.repository.ResourceActionTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Optional;

/** Persists confirmed workflow transitions; callers keep it in the aggregate transaction. */
@Service
public class ResourceActionTransitionService {
    private final ResourceActionTransitionRepository repository;

    public ResourceActionTransitionService(ResourceActionTransitionRepository repository) {
        this.repository = repository;
    }

    public UUID record(
            String resourceKey, Object resourceId, String actionId, String actionScope, String previousState, String resultingState,
            String reasonCode, String comment, LocalDate effectiveAt, String actorSubject, String correlationId,
            Long versionBefore, Long versionAfter
    ) {
        UUID transitionId = UUID.randomUUID();
        repository.save(new ResourceActionTransition(
                transitionId, resourceKey, String.valueOf(resourceId), actionId, actionScope, previousState, resultingState,
                reasonCode, comment, effectiveAt, Instant.now(), actorSubject, currentAuthorities(), correlationId,
                currentHeader("X-Request-ID"), currentHeader("Idempotency-Key"), versionBefore, versionAfter
        ));
        return transitionId;
    }

    public Optional<UUID> findReplay(String resourceKey, Object resourceId, String actionId) {
        String idempotencyKey = currentHeader("Idempotency-Key");
        if (idempotencyKey == null) return Optional.empty();
        return repository.findByResourceKeyAndResourceIdAndActionIdAndActorSubjectAndIdempotencyKey(
                resourceKey, String.valueOf(resourceId), actionId, currentActorSubject(), idempotencyKey)
                .map(ResourceActionTransition::getTransitionId);
    }

    private String currentAuthorities() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) return null;
        return authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String currentActorSubject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "anonymous"
                : authentication.getName().trim();
    }

    private String currentHeader(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader(name);
            return value == null || value.isBlank() ? null : value.trim();
        }
        return null;
    }
}
