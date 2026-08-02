package com.example.praxis.apiquickstart.hr.options;

import com.example.praxis.apiquickstart.config.QuickstartOptionSourceContextResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioRepository;
import com.example.praxis.apiquickstart.hr.service.FuncionarioService;
import org.praxisplatform.uischema.dto.OptionDTO;
import org.praxisplatform.uischema.options.LookupSortOption;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionContext;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionRequest;
import org.praxisplatform.uischema.options.service.OptionSourceOperation;
import org.praxisplatform.uischema.options.service.OptionSourceProvider;
import org.springframework.core.Ordered;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Private execution adapter for the canonical employee option source.
 *
 * <p>The public contract declares only search intents. This provider owns the JPA bindings,
 * applies the server-resolved department scope before every query and publishes only a masked
 * document suffix in the human label.</p>
 */
@Component
public class EmployeeOptionSourceProvider implements OptionSourceProvider, Ordered {
    private static final String EMPLOYEE_CODE = "employee-code";
    private static final String NAME = "name";
    private static final String DOCUMENT = "document";

    private final FuncionarioRepository repository;

    public EmployeeOptionSourceProvider(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(
            OptionSourceDescriptor descriptor,
            OptionSourceExecutionContext context,
            OptionSourceOperation operation
    ) {
        return descriptor != null
                && FuncionarioService.EMPLOYEE_OPTION_SOURCE_KEY.equals(descriptor.key())
                && ApiPaths.HumanResources.FUNCIONARIOS.equals(descriptor.resourcePath())
                && operation == context.operation();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OptionDTO<Object>> filter(OptionSourceExecutionRequest<?> request) {
        Specification<Funcionario> specification = authorizedScope(request.context());
        if (request.search() != null) {
            specification = specification.and(searchSpecification(request.searchStrategy(), request.search()));
        }
        Pageable pageable = governedPageable(request);
        return repository.findAll(specification, pageable).map(EmployeeOptionSourceProvider::toOption);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDTO<Object>> byIds(OptionSourceExecutionRequest<?> request) {
        Specification<Funcionario> specification = authorizedScope(request.context());
        List<Integer> ids = integerIds(request.ids());
        if (ids.isEmpty()) {
            return List.of();
        }
        specification = specification.and((root, query, criteriaBuilder) -> root.get("id").in(ids));
        return repository.findAll(specification, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(EmployeeOptionSourceProvider::toOption)
                .toList();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Specification<Funcionario> authorizedScope(OptionSourceExecutionContext context) {
        Map<String, Object> attributes = context.attributes();
        Object subject = attributes.get(QuickstartOptionSourceContextResolver.AUTHENTICATED_SUBJECT);
        if (!(subject instanceof String value) || value.isBlank()) {
            throw new AccessDeniedException("Employee option source requires an authenticated context.");
        }
        if (Boolean.TRUE.equals(attributes.get(QuickstartOptionSourceContextResolver.DEPARTMENT_SCOPE_UNBOUNDED))) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        Set<Integer> departmentIds = integerSet(
                attributes.get(QuickstartOptionSourceContextResolver.DEPARTMENT_SCOPE_IDS));
        if (departmentIds.isEmpty()) {
            throw new AccessDeniedException("Employee option source has no authorized department scope.");
        }
        return (root, query, criteriaBuilder) -> root.get("departamento").get("id").in(departmentIds);
    }

    private Specification<Funcionario> searchSpecification(String strategy, String search) {
        return switch (strategy) {
            case EMPLOYEE_CODE -> {
                int employeeId;
                try {
                    employeeId = Integer.parseInt(search);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Employee code is outside the supported numeric range.");
                }
                int effectiveId = employeeId;
                yield (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), effectiveId);
            }
            case NAME -> {
                String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
                yield (root, query, criteriaBuilder) -> criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nomeCompleto")), pattern, '\\');
            }
            case DOCUMENT -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("cpf"), search);
            default -> throw new IllegalArgumentException("Unsupported employee lookup search strategy.");
        };
    }

    private Pageable governedPageable(OptionSourceExecutionRequest<?> request) {
        Pageable requested = request.pageable();
        int page = requested == null || requested.isUnpaged() ? 0 : requested.getPageNumber();
        int size = requested == null || requested.isUnpaged()
                ? request.descriptor().policy().defaultPageSize()
                : requested.getPageSize();
        String requestedSortKey = request.sortKey();
        String sortKey = requestedSortKey == null
                ? request.descriptor().effectiveFiltering().defaultSort()
                : requestedSortKey;
        LookupSortOption option = request.descriptor().effectiveFiltering().sortOptions().stream()
                .filter(candidate -> candidate.key().equals(sortKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Employee lookup sort key is not governed."));
        Sort.Direction direction = "desc".equals(option.direction()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, option.field()).and(Sort.by("id")));
    }

    private static OptionDTO<Object> toOption(Funcionario employee) {
        Map<String, Object> extra = new LinkedHashMap<>();
        boolean selectable = Boolean.TRUE.equals(employee.getAtivo());
        extra.put("selectable", selectable);
        if (!selectable) {
            extra.put("disabledReason", "Funcionario inativo preservado apenas para valores existentes.");
        }
        extra.put("entityKey", FuncionarioService.EMPLOYEE_OPTION_SOURCE_KEY);
        extra.put("resourcePath", ApiPaths.HumanResources.FUNCIONARIOS);
        return new OptionDTO<>(employee.getId(), maskedLabel(employee), Map.copyOf(extra));
    }

    private static String maskedLabel(Funcionario employee) {
        String cpf = employee.getCpf();
        String suffix = cpf != null && cpf.length() >= 2 ? cpf.substring(cpf.length() - 2) : "**";
        return employee.getNomeCompleto() + " · CPF ***.***.***-" + suffix;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<Integer> integerIds(Collection<Object> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(EmployeeOptionSourceProvider::integerValue)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    private static Set<Integer> integerSet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }
        return collection.stream()
                .map(EmployeeOptionSourceProvider::integerValue)
                .filter(item -> item != null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || !String.valueOf(value).matches("[0-9]+")) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
