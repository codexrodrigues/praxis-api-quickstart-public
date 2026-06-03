package com.example.praxis.apiquickstart.hr.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuncionarioTest {

    @Test
    void shouldStoreCpfWithoutMaskCharacters() {
        Funcionario funcionario = new Funcionario();

        funcionario.setCpf("123.456.789-01");

        assertEquals("12345678901", funcionario.getCpf());
    }

    @Test
    void shouldKeepNullCpfAsNull() {
        Funcionario funcionario = new Funcionario();

        funcionario.setCpf(null);

        assertNull(funcionario.getCpf());
    }
}
