package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.mapper.FuncionarioMapper;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourceType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceOptionSourceTest {

    @Mock
    private FuncionarioRepository repository;

    @Mock
    private FuncionarioMapper mapper;

    @Mock
    private CollectionExportExecutor collectionExportExecutor;

    @Test
    void shouldExposeEmployeeEntityLookupAsCanonicalOptionSource() {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);

        OptionSourceDescriptor descriptor = service.getOptionSourceRegistry()
                .resolve(Funcionario.class, FuncionarioService.EMPLOYEE_OPTION_SOURCE_KEY)
                .orElseThrow();
        EntityLookupDescriptor lookup = descriptor.entityLookup();

        assertEquals(OptionSourceType.RESOURCE_ENTITY, descriptor.type());
        assertEquals(ApiPaths.HumanResources.FUNCIONARIOS, descriptor.resourcePath());
        assertNull(descriptor.filterField());
        assertEquals("id", descriptor.valuePropertyPath());
        assertEquals("nomeCompleto", descriptor.labelPropertyPath());
        assertEquals("employee", lookup.entityKey());
        assertNull(lookup.codePropertyPath());
        assertEquals(List.of("nomeCompleto", "cargo.nome", "departamento.nome"), lookup.searchPropertyPaths());
        assertEquals(List.of("cargo.nome", "departamento.nome"), lookup.descriptionPropertyPaths());
        assertEquals("ativo", lookup.selectionPolicy().selectablePropertyPath());
        assertTrue(lookup.capabilities().filter());
        assertTrue(lookup.capabilities().byIds());
        assertTrue(lookup.capabilities().navigateToDetail());
        assertFalse(lookup.capabilities().create());
        assertEquals("surface", lookup.detail().kind());
        assertEquals("view", lookup.detail().surfaceId());
        assertEquals("drawer", lookup.detail().presentation());
        assertNull(lookup.detail().routeTemplate());
        assertEquals("directory", lookup.display().preset());
        assertEquals("compact", lookup.display().selectedLayout());
        assertEquals(List.of("cargo.nome", "departamento.nome", "dataAdmissao"), lookup.display().secondaryPropertyPaths());
        assertEquals(3, lookup.display().fields().size());
        assertEquals("role", lookup.display().fields().get(0).key());
        assertEquals("cargo.nome", lookup.display().fields().get(0).propertyPath());
        assertEquals("work", lookup.display().fields().get(0).icon());
        assertEquals("text", lookup.display().fields().get(0).presentation());
        assertEquals("admissionDate", lookup.display().fields().get(2).key());
        assertEquals("date", lookup.display().fields().get(2).presentation());
        assertEquals("date", lookup.display().fields().get(2).format());
    }
}
