package com.example.praxis.apiquickstart.hr.dto.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.filter.annotation.Filterable;

class FuncionarioFilterDTOTest {

    @Test
    void exposesAnalyticalLabelsAsFilterableRelationshipFields() throws Exception {
        assertFilterableRelationship("departamentoNome", "departamento.nome");
        assertFilterableRelationship("cargoNome", "cargo.nome");
    }

    private void assertFilterableRelationship(String fieldName, String relation) throws Exception {
        Filterable filterable = FuncionarioFilterDTO.class
                .getDeclaredField(fieldName)
                .getAnnotation(Filterable.class);

        assertNotNull(filterable, fieldName + " must remain filterable for chart drilldown dashboards");
        assertEquals(Filterable.FilterOperation.LIKE, filterable.operation());
        assertEquals(relation, filterable.relation());
    }
}
