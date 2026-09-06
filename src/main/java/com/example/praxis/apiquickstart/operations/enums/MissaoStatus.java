package com.example.praxis.apiquickstart.operations.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vocabulário canônico do ciclo de vida de uma missão.
 *
 * <p>O código do enum continua sendo a identidade persistida e usada em filtros.
 * O rótulo serve exclusivamente às projeções e superfícies de apresentação.</p>
 */
public enum MissaoStatus {
    PLANEJADA("Planejada"),
    EM_ANDAMENTO("Em andamento"),
    PAUSADA("Pausada"),
    CONCLUIDA("Concluída"),
    FALHOU("Falhou");

    /**
     * Expressão SQL usada pela projeção analítica read-only.
     *
     * <p>Ela fica ao lado do vocabulário para impedir que a view JPA, o option
     * source e os consumidores mantenham catálogos independentes.</p>
     */
    public static final String SQL_LABEL_EXPRESSION =
            "CASE status "
                    + "WHEN 'PLANEJADA' THEN 'Planejada' "
                    + "WHEN 'EM_ANDAMENTO' THEN 'Em andamento' "
                    + "WHEN 'PAUSADA' THEN 'Pausada' "
                    + "WHEN 'CONCLUIDA' THEN 'Concluída' "
                    + "WHEN 'FALHOU' THEN 'Falhou' "
                    + "ELSE status END";

    private static final Map<String, String> LABELS = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, MissaoStatus::label));

    private final String label;

    MissaoStatus(String label) {
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

