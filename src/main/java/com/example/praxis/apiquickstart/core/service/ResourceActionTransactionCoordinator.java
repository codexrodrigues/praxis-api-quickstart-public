package com.example.praxis.apiquickstart.core.service;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirma a mutacao de dominio e o resultado idempotente na mesma transacao operacional. */
@Service
public class ResourceActionTransactionCoordinator {
    private final ResourceActionExecutionService executionService;

    public ResourceActionTransactionCoordinator(ResourceActionExecutionService executionService) {
        this.executionService = executionService;
    }

    @Transactional
    public <T> T execute(ResourceActionExecution execution, Supplier<T> operation) {
        T result = operation.get();
        executionService.complete(execution, result);
        return result;
    }
}
