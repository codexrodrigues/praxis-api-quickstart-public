package com.example.praxis.apiquickstart.procurement.options;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.dto.OptionDTO;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionContext;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionRequest;
import org.praxisplatform.uischema.options.service.OptionSourceOperation;
import org.praxisplatform.uischema.options.service.OptionSourceProvider;
import org.springframework.core.Ordered;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Neutral non-JPA provider example for hosts that execute a public Praxis option source
 * against a private external catalog.
 */
@Component
public class ExternalCatalogOptionSourceProvider implements OptionSourceProvider, Ordered {

    private static final List<OptionDTO<Object>> OPTIONS = List.of(
            option("EXT-CAT-A", "External catalog A", "general"),
            option("EXT-CAT-B", "External catalog B", "restricted"),
            option("EXT-CAT-C", "External catalog C", "general")
    );
    private static final Map<String, OptionDTO<Object>> OPTIONS_BY_ID = Map.of(
            "EXT-CAT-A", OPTIONS.get(0),
            "EXT-CAT-B", OPTIONS.get(1),
            "EXT-CAT-C", OPTIONS.get(2)
    );

    @Override
    public boolean supports(
            OptionSourceDescriptor descriptor,
            OptionSourceExecutionContext context,
            OptionSourceOperation operation
    ) {
        return descriptor != null
                && ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE.equals(descriptor.key())
                && operation == context.operation();
    }

    @Override
    public Page<OptionDTO<Object>> filter(OptionSourceExecutionRequest<?> request) {
        String search = request.search() == null ? "" : request.search().toLowerCase(Locale.ROOT);
        Object companyId = filterValue(request.filterPayload(), "companyId");
        List<OptionDTO<Object>> content = OPTIONS.stream()
                .filter(option -> isAvailableForCompany(companyId, option.id()))
                .filter(option -> search.isBlank()
                        || option.label().toLowerCase(Locale.ROOT).contains(search)
                        || String.valueOf(option.id()).toLowerCase(Locale.ROOT).contains(search))
                .toList();
        return new PageImpl<>(content, request.pageable(), content.size());
    }

    @Override
    public List<OptionDTO<Object>> byIds(OptionSourceExecutionRequest<?> request) {
        return request.ids().stream()
                .map(String::valueOf)
                .map(OPTIONS_BY_ID::get)
                .filter(option -> option != null)
                .toList();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static OptionDTO<Object> option(String id, String label, String category) {
        return new OptionDTO<>(id, label, Map.of(
                "category", category,
                "selectable", true
        ));
    }

    private static boolean isAvailableForCompany(Object companyId, Object externalId) {
        if (companyId == null) {
            return true;
        }
        String company = String.valueOf(companyId);
        String id = String.valueOf(externalId);
        return switch (company) {
            case "1" -> true;
            case "2" -> !"EXT-CAT-B".equals(id);
            default -> false;
        };
    }

    private static Object filterValue(Object filterPayload, String fieldName) {
        if (filterPayload instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        if (filterPayload == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method getter = filterPayload.getClass().getMethod(getterName);
            return getter.invoke(filterPayload);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
