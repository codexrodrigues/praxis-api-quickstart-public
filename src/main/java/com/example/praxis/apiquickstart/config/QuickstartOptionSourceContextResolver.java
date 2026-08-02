package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeProvider;
import com.example.praxis.apiquickstart.hr.service.FuncionarioService;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.service.OptionSourceContextResolver;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionContext;
import org.praxisplatform.uischema.options.service.OptionSourceOperation;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Supplies authenticated, server-resolved context to option-source providers.
 *
 * <p>The subject and HR department scope are deliberately private execution attributes. They are
 * never copied to OpenAPI, {@code x-ui.optionSource}, responses or diagnostics.</p>
 */
@Component
public class QuickstartOptionSourceContextResolver implements OptionSourceContextResolver {
    public static final String AUTHENTICATED_SUBJECT = "authenticatedSubject";
    public static final String DEPARTMENT_SCOPE_UNBOUNDED = "departmentScopeUnbounded";
    public static final String DEPARTMENT_SCOPE_IDS = "departmentScopeIds";

    private final HrDepartmentScopeProvider departmentScopeProvider;

    public QuickstartOptionSourceContextResolver(HrDepartmentScopeProvider departmentScopeProvider) {
        this.departmentScopeProvider = departmentScopeProvider;
    }

    @Override
    public OptionSourceExecutionContext resolve(
            OptionSourceDescriptor descriptor,
            OptionSourceOperation operation
    ) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            String subject = authentication.getName();
            attributes.put(AUTHENTICATED_SUBJECT, subject);
            if (isEmployeeSource(descriptor)) {
                var departmentScope = departmentScopeProvider.departmentsFor(subject);
                attributes.put(DEPARTMENT_SCOPE_UNBOUNDED, departmentScope.isEmpty());
                attributes.put(DEPARTMENT_SCOPE_IDS, departmentScope.map(Set::copyOf).orElseGet(Set::of));
            }
        }
        return new OptionSourceExecutionContext(
                descriptor.key(),
                descriptor.type(),
                descriptor.resourcePath(),
                operation,
                attributes
        );
    }

    private static boolean isEmployeeSource(OptionSourceDescriptor descriptor) {
        return FuncionarioService.EMPLOYEE_OPTION_SOURCE_KEY.equals(descriptor.key())
                && ApiPaths.HumanResources.FUNCIONARIOS.equals(descriptor.resourcePath());
    }
}
