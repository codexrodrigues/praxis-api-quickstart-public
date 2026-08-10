package com.example.praxis.apiquickstart.hr.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FuncionarioControllerMetadataTest {

    @Test
    void readOperationShouldPublishTaskOrientedCopy() throws Exception {
        Method method = FuncionarioController.class.getMethod("getById", Integer.class);
        Operation operation = method.getAnnotation(Operation.class);

        assertNotNull(operation);
        assertEquals("Detalhes do funcionário", operation.summary());
        assertEquals(
                "Consulte os dados cadastrais, contatos, vínculo, cargo e remuneração da pessoa selecionada.",
                operation.description());
    }
}
