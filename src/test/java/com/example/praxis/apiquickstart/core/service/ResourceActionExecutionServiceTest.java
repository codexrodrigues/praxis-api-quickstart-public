package com.example.praxis.apiquickstart.core.service;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.repository.ResourceActionExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.praxisplatform.uischema.action.ActionScope;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceActionExecutionServiceTest {
    @Test void replaysWinnerAfterUniqueKeyRace() {
        var repository = mock(ResourceActionExecutionRepository.class);
        var service = new ResourceActionExecutionService(repository, new ObjectMapper());
        var winner = execution("key", hash(new ObjectMapper(), java.util.Map.of("x", 1)));
        winner.complete(new ObjectMapper().valueToTree(java.util.Map.of("result", "done")));
        when(repository.findByResourceKeyAndActionIdAndIdempotencyKey("r", "a", "key")).thenReturn(Optional.of(winner));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertSame(winner, service.reserve("r", "a", ActionScope.COLLECTION, "key", java.util.Map.of("x", 1), "correlation", "operator").orElseThrow());
    }
    @Test void rejectsSameKeyWithDifferentCommand() {
        var mapper = new ObjectMapper(); var repository = mock(ResourceActionExecutionRepository.class); var service = new ResourceActionExecutionService(repository, mapper);
        var winner = execution("key", hash(mapper, java.util.Map.of("x", 1)));
        when(repository.findByResourceKeyAndActionIdAndIdempotencyKey("r", "a", "key")).thenReturn(Optional.of(winner));
        assertThrows(ResponseStatusException.class, () -> service.findCompletedReplay("r", "a", "key", java.util.Map.of("x", 2)));
    }
    private static ResourceActionExecution execution(String key, String requestHash) {
        return new ResourceActionExecution(java.util.UUID.randomUUID(), "r", "a", "COLLECTION", key, requestHash,
                "correlation", null, "operator", "ROLE_OPERATOR");
    }
    private static String hash(ObjectMapper mapper, Object value) { try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(value))); } catch (Exception e) { throw new RuntimeException(e); } }
}
