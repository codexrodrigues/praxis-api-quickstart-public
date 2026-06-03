package com.example.praxis.apiquickstart.hr.dto;

import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuncionarioDtoGovernanceProjectionTest {

    @Test
    void governedPersonalAndFinancialFieldsAreHiddenFromDefaultTables() throws Exception {
        for (String fieldName : List.of("cpf", "dataNascimento", "email", "telefone", "salario")) {
            Field field = FuncionarioDTO.class.getDeclaredField(fieldName);

            assertNotNull(field.getAnnotation(DomainGovernance.class),
                    fieldName + " deve continuar publicado com governanca semantica");

            UISchema uiSchema = field.getAnnotation(UISchema.class);
            assertNotNull(uiSchema, fieldName + " deve continuar publicado com x-ui");
            assertTrue(uiSchema.tableHidden(),
                    fieldName + " nao deve aparecer em tabelas padrao geradas por IA");
        }
    }
}
