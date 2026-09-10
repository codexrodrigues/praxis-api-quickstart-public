package com.example.praxis.apiquickstart.hr.options;

import com.example.praxis.apiquickstart.config.QuickstartOptionSourceContextResolver;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionContext;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionRequest;
import org.praxisplatform.uischema.options.service.OptionSourceOperation;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeOptionSourceProviderPolicyTest {
    private final FuncionarioRepository repository = mock(FuncionarioRepository.class);

    @Test
    void corporateModeRejectsAnonymousFilterAndEvenEmptyReloadBeforeQuerying() {
        var provider = new EmployeeOptionSourceProvider(repository, false);
        var request = request(Map.of());
        assertThrows(AccessDeniedException.class, () -> provider.filter(request));
        assertThrows(AccessDeniedException.class, () -> provider.byIds(request));
        verifyNoInteractions(repository);
    }

    @Test
    void publicDemoAllowsAnonymousEmptyReloadWithoutInventingAnIdentity() {
        var request = request(Map.of());
        assertTrue(new EmployeeOptionSourceProvider(repository, true).byIds(request).isEmpty());
        assertTrue(request.context().attributes().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void publicDemoPreservesDenyAllDepartmentScopeForAuthenticatedCallers() {
        var provider = new EmployeeOptionSourceProvider(repository, true);
        var request = request(Map.of(
                QuickstartOptionSourceContextResolver.AUTHENTICATED_SUBJECT, "scoped-user",
                QuickstartOptionSourceContextResolver.DEPARTMENT_SCOPE_IDS, Set.of()));
        assertThrows(AccessDeniedException.class, () -> provider.filter(request));
        assertThrows(AccessDeniedException.class, () -> provider.byIds(request));
        verifyNoInteractions(repository);
    }

    private OptionSourceExecutionRequest<Object> request(Map<String, Object> attributes) {
        var context = new OptionSourceExecutionContext("employee", OptionSourceType.RESOURCE_ENTITY,
                "/api/human-resources/funcionarios", OptionSourceOperation.BY_IDS, attributes);
        return new OptionSourceExecutionRequest<>(null, null, null, null, null,
                List.of(), null, null, List.of(), List.of(), context);
    }
}
