package com.example.praxis.apiquickstart.operations.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissaoVocabularyTest {

    @Test
    void shouldPublishOneHumanLabelForEveryCanonicalMissionStatus() {
        Set<String> canonicalCodes = Arrays.stream(MissaoStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(canonicalCodes, MissaoStatus.labels().keySet());
        assertEquals("Em andamento", MissaoStatus.labelForCode("EM_ANDAMENTO"));
        assertEquals("STATUS_FUTURO", MissaoStatus.labelForCode("STATUS_FUTURO"));
        assertNull(MissaoStatus.labelForCode(null));
        assertThrows(UnsupportedOperationException.class,
                () -> MissaoStatus.labels().put("STATUS_FUTURO", "Status futuro"));
        MissaoStatus.labels().forEach((code, label) -> {
            assertTrue(MissaoStatus.SQL_LABEL_EXPRESSION.contains("'" + code + "'"));
            assertTrue(MissaoStatus.SQL_LABEL_EXPRESSION.contains("'" + label + "'"));
        });
    }

    @Test
    void shouldPublishOneHumanLabelForEveryCanonicalMissionPriority() {
        Set<String> canonicalCodes = Arrays.stream(MissaoPrioridade.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(canonicalCodes, MissaoPrioridade.labels().keySet());
        assertEquals("Crítica", MissaoPrioridade.labelForCode("CRITICA"));
        assertEquals("PRIORIDADE_FUTURA", MissaoPrioridade.labelForCode("PRIORIDADE_FUTURA"));
        assertNull(MissaoPrioridade.labelForCode(null));
        assertThrows(UnsupportedOperationException.class,
                () -> MissaoPrioridade.labels().put("PRIORIDADE_FUTURA", "Prioridade futura"));
        MissaoPrioridade.labels().forEach((code, label) -> {
            assertTrue(MissaoPrioridade.SQL_LABEL_EXPRESSION.contains("'" + code + "'"));
            assertTrue(MissaoPrioridade.SQL_LABEL_EXPRESSION.contains("'" + label + "'"));
        });
    }
}
