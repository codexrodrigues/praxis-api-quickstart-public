package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicDemoEmployeeAddressFixtureContractTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path PUBLIC_DEMO_SEED = PROJECT_ROOT.resolve("db/dump/public-demo-seed.sql");
    private static final Path DATASET_GUARD_BOOTSTRAP = PROJECT_ROOT.resolve(
            "db/demo-seeds/public-demo/employee-address-coherence/"
                    + "V20260812_000__public_demo_dataset_guard_bootstrap.sql");
    private static final Path ADDRESS_PATCH = PROJECT_ROOT.resolve(
            "db/demo-seeds/public-demo/employee-address-coherence/"
                    + "V20260812_001__employee_address_coherence.sql");

    @Test
    void publicDemoKeepsPepperPottsAddressCoherentWithHerIdentity() throws IOException {
        String seed = Files.readString(PUBLIC_DEMO_SEED, StandardCharsets.UTF_8);

        assertTrue(seed.contains(
                "2\tAvenida Park\t10880\tApartamento 42\tMidtown\tNova York\tNY\t10017-000\t2"));
        assertFalse(seed.contains(
                "2\tAlameda das Mansões\t1\tMansão Wayne\tNobre\tGotham\tGT\t54321-000\t2"));
    }

    @Test
    void legacyPublicDemoBootstrapRequiresTheImmutableFixtureBeforePublishingTheFingerprint()
            throws IOException {
        String bootstrap = Files.readString(DATASET_GUARD_BOOTSTRAP, StandardCharsets.UTF_8);

        assertTrue(bootstrap.contains("to_regclass('public.praxis_demo_dataset_guard') is null"));
        assertTrue(bootstrap.contains("id between 1 and 50"));
        assertTrue(bootstrap.contains("id between 1 and 27"));
        assertTrue(bootstrap.contains("nome_completo = 'Tony Stark'"));
        assertTrue(bootstrap.contains("nome_completo = 'Pepper Potts'"));
        assertTrue(bootstrap.contains("nome_completo = 'Bruce Wayne'"));
        assertTrue(bootstrap.contains("nome_completo = 'Maria Hill'"));
        assertTrue(bootstrap.contains("praxis-public-demo-2026-07-15"));
        assertTrue(bootstrap.contains("PRAXIS_DEMO_GUARD"));
    }

    @Test
    void optInPatchFailsClosedOutsideTheRecognizedPublicDemo() throws IOException {
        String patch = Files.readString(ADDRESS_PATCH, StandardCharsets.UTF_8);

        assertTrue(patch.contains("praxis-public-demo-2026-07-15"));
        assertTrue(patch.contains("nome_completo = 'Pepper Potts'"));
        assertTrue(patch.contains("cpf = '90000000256'"));
        assertTrue(patch.contains("PRAXIS_DEMO_GUARD"));
        assertTrue(patch.contains("where id = 2"));
        assertTrue(patch.contains("and funcionario_id = 2"));
        assertTrue(patch.contains("and logradouro = 'Alameda das Mansões'"));
        assertFalse(patch.contains("or (\n          logradouro = 'Avenida Park'"));
    }
}
