package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicDemoEmployeeDependentFixtureContractTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path PUBLIC_DEMO_SEED = PROJECT_ROOT.resolve("db/dump/public-demo-seed.sql");
    private static final Path DEPENDENT_PATCH = PROJECT_ROOT.resolve(
            "db/demo-seeds/public-demo/employee-dependent-coherence/"
                    + "V20260812_001__employee_dependent_coherence.sql");

    @Test
    void publicDemoKeepsOnlyTheCanonicalMorganStarkDependent() throws IOException {
        String seed = Files.readString(PUBLIC_DEMO_SEED, StandardCharsets.UTF_8);
        String canonical = "1\tMorgan Stark\tFilha\t2015-04-01\t1";

        assertEquals(1, countOccurrences(seed, canonical));
        assertFalse(seed.contains("68\tMorgan Stark\tFILHA\t2018-05-04\t1"));
    }

    @Test
    void optInPatchDeletesOnlyTheExactRecognizedDuplicateAndFailsClosed() throws IOException {
        String patch = Files.readString(DEPENDENT_PATCH, StandardCharsets.UTF_8);

        assertTrue(patch.contains("nome_completo = 'Tony Stark'"));
        assertTrue(patch.contains("cpf = '90000000175'"));
        assertTrue(patch.contains("email = 'tony.stark@stark.demo.praxisui.dev'"));
        assertTrue(patch.contains("delete from public.dependentes"));
        assertTrue(patch.contains("where id = 68"));
        assertTrue(patch.contains("and nome_completo = 'Morgan Stark'"));
        assertTrue(patch.contains("and parentesco = 'FILHA'"));
        assertTrue(patch.contains("and data_nascimento = date '2018-05-04'"));
        assertTrue(patch.contains("and funcionario_id = 1"));
        assertTrue(patch.contains("PRAXIS_DEMO_GUARD"));
        assertFalse(patch.contains("where nome_completo = 'Morgan Stark';"));
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
