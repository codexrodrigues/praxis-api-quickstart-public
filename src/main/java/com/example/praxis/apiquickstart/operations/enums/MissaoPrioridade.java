package com.example.praxis.apiquickstart.operations.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vocabulário canônico da prioridade operacional de uma missão.
 */
public enum MissaoPrioridade {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    CRITICA("Crítica");

    public static final String SQL_LABEL_EXPRESSION =
            "CASE prioridade "
                    + "WHEN 'BAIXA' THEN 'Baixa' "
                    + "WHEN 'MEDIA' THEN 'Média' "
                    + "WHEN 'ALTA' THEN 'Alta' "
                    + "WHEN 'CRITICA' THEN 'Crítica' "
                    + "ELSE prioridade END";

    private static final Map<String, String> LABELS = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, MissaoPrioridade::label));

    private final String label;

    MissaoPrioridade(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Map<String, String> labels() {
        return LABELS;
    }

    public static String labelForCode(String code) {
        if (code == null) {
            return null;
        }
        return LABELS.getOrDefault(code, code);
    }
}

