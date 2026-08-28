package com.example.praxis.apiquickstart.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Origens dos consumidores browser publicados oficialmente pela plataforma de referencia.
 *
 * <p>Esse baseline vive em codigo para que variaveis de ambiente do deployment possam apenas
 * adicionar consumidores. Propriedades Spring sao sobrescreviveis por relaxed binding e, por
 * isso, nao podem proteger sozinhas a conectividade entre superficies oficiais.</p>
 */
public final class OfficialBrowserOrigins {
    private static final List<String> BASELINE = List.of(
            "https://praxisui.dev",
            "https://praxis-ui-4e602.web.app",
            "https://praxis-ui-4e602.firebaseapp.com",
            "https://praxis-policy-studio-homolog.onrender.com"
    );

    private OfficialBrowserOrigins() {
    }

    public static List<String> baseline() {
        return BASELINE;
    }

    public static Set<String> mergeWith(String additionalOrigins) {
        LinkedHashSet<String> origins = new LinkedHashSet<>(BASELINE);
        if (additionalOrigins != null) {
            Arrays.stream(additionalOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(origins::add);
        }
        return origins;
    }
}
