package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_resource_entity_governance_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_resource_entity_governance_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class ResourceEntityLookupGovernanceIntegrationTest {

    private static final Map<String, String> EXPECTED_RESOURCE_ENTITY_SOURCES = new LinkedHashMap<>();
    private static final Map<String, SchemaLookupCase> SCHEMA_LOOKUP_CASES = new LinkedHashMap<>();

    static {
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.FUNCIONARIOS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.HumanResources.FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_SOURCE,
                ApiPaths.HumanResources.FOLHAS_PAGAMENTO
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                ApiPaths.RiskIntelligence.AMEACAS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Operations.BASES_BASE_LOOKUP_SOURCE,
                ApiPaths.Operations.BASES
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_SOURCE,
                ApiPaths.Operations.EQUIPES
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE,
                ApiPaths.Operations.ACORDOS_REGULATORIOS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_SOURCE,
                ApiPaths.Operations.INCIDENTES
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Operations.MISSOES_MISSION_LOOKUP_SOURCE,
                ApiPaths.Operations.MISSOES
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                ApiPaths.Assets.EQUIPAMENTOS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                ApiPaths.Assets.VEICULOS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                ApiPaths.Procurement.CONTRACTS
        );
        EXPECTED_RESOURCE_ENTITY_SOURCES.put(
                ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_SOURCE,
                ApiPaths.Procurement.PRODUCTS
        );

        SCHEMA_LOOKUP_CASES.put("employee", new SchemaLookupCase(
                ApiPaths.Operations.MISSAO_PARTICIPANTES,
                "funcionarioId",
                ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
                ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_SOURCE,
                ApiPaths.HumanResources.FUNCIONARIOS
        ));
        SCHEMA_LOOKUP_CASES.put("payroll", new SchemaLookupCase(
                ApiPaths.HumanResources.EVENTOS_FOLHA,
                "folhaPagamentoId",
                ApiPaths.HumanResources.FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_OPTIONS,
                ApiPaths.HumanResources.FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_SOURCE,
                ApiPaths.HumanResources.FOLHAS_PAGAMENTO
        ));
        SCHEMA_LOOKUP_CASES.put("threat", new SchemaLookupCase(
                ApiPaths.Operations.MISSOES,
                "ameacaId",
                ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_OPTIONS,
                ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                ApiPaths.RiskIntelligence.AMEACAS
        ));
        SCHEMA_LOOKUP_CASES.put("base", new SchemaLookupCase(
                ApiPaths.Operations.EQUIPES,
                "basePrincipalId",
                ApiPaths.Operations.BASES_BASE_LOOKUP_OPTIONS,
                ApiPaths.Operations.BASES_BASE_LOOKUP_SOURCE,
                ApiPaths.Operations.BASES
        ));
        SCHEMA_LOOKUP_CASES.put("team", new SchemaLookupCase(
                ApiPaths.Operations.EQUIPE_MEMBROS,
                "equipeId",
                ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS,
                ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_SOURCE,
                ApiPaths.Operations.EQUIPES
        ));
        SCHEMA_LOOKUP_CASES.put("agreement", new SchemaLookupCase(
                ApiPaths.Operations.LICENCAS_OPERACAO,
                "acordoId",
                ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_OPTIONS,
                ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE,
                ApiPaths.Operations.ACORDOS_REGULATORIOS
        ));
        SCHEMA_LOOKUP_CASES.put("incident", new SchemaLookupCase(
                ApiPaths.HumanResources.INDENIZACOES,
                "incidenteId",
                ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_OPTIONS,
                ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_SOURCE,
                ApiPaths.Operations.INCIDENTES
        ));
        SCHEMA_LOOKUP_CASES.put("mission", new SchemaLookupCase(
                ApiPaths.Operations.INCIDENTES,
                "missaoId",
                ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
                ApiPaths.Operations.MISSOES_MISSION_LOOKUP_SOURCE,
                ApiPaths.Operations.MISSOES
        ));
        SCHEMA_LOOKUP_CASES.put("equipment", new SchemaLookupCase(
                ApiPaths.Assets.EQUIPAMENTO_ALOCACOES,
                "equipamentoId",
                ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_OPTIONS,
                ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                ApiPaths.Assets.EQUIPAMENTOS
        ));
        SCHEMA_LOOKUP_CASES.put("vehicle", new SchemaLookupCase(
                ApiPaths.Assets.VEICULO_MISSAO_USOS,
                "veiculoId",
                ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_OPTIONS,
                ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                ApiPaths.Assets.VEICULOS
        ));
        SCHEMA_LOOKUP_CASES.put("company", new SchemaLookupCase(
                ApiPaths.Procurement.SUPPLIERS,
                "companyId",
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        ));
        SCHEMA_LOOKUP_CASES.put("supplier", new SchemaLookupCase(
                ApiPaths.Procurement.CONTRACTS,
                "supplierId",
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS,
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS
        ));
        SCHEMA_LOOKUP_CASES.put("contract", new SchemaLookupCase(
                ApiPaths.Procurement.PRODUCTS,
                "contractId",
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS,
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                ApiPaths.Procurement.CONTRACTS
        ));
        SCHEMA_LOOKUP_CASES.put("product", new SchemaLookupCase(
                ApiPaths.Procurement.PURCHASE_ORDERS,
                "productId",
                ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_OPTIONS,
                ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_SOURCE,
                ApiPaths.Procurement.PRODUCTS
        ));
    }

    @Autowired
    private OptionSourceRegistry optionSourceRegistry;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldGovernAllRegisteredResourceEntityDescriptors() {
        Map<String, OptionSourceDescriptor> resourceEntityDescriptors = optionSourceRegistry.descriptors()
                .stream()
                .filter(descriptor -> descriptor.type() == OptionSourceType.RESOURCE_ENTITY)
                .collect(Collectors.toMap(
                        OptionSourceDescriptor::key,
                        descriptor -> descriptor,
                        (left, right) -> {
                            throw new AssertionError("Duplicate RESOURCE_ENTITY source key: " + left.key());
                        },
                        LinkedHashMap::new
                ));

        assertEquals(EXPECTED_RESOURCE_ENTITY_SOURCES.keySet(), resourceEntityDescriptors.keySet());
        assertEquals(
                EXPECTED_RESOURCE_ENTITY_SOURCES.entrySet()
                        .stream()
                        .map(entry -> entry.getValue() + "#" + entry.getKey())
                        .collect(Collectors.toSet()),
                resourceEntityDescriptors.values()
                        .stream()
                        .map(descriptor -> descriptor.resourcePath() + "#" + descriptor.key())
                        .collect(Collectors.toSet())
        );

        resourceEntityDescriptors.forEach((key, descriptor) -> {
            assertEquals(EXPECTED_RESOURCE_ENTITY_SOURCES.get(key), descriptor.resourcePath(), key);
            assertNotNull(descriptor.entityLookup(), key);
            assertEquals(key, descriptor.entityLookup().entityKey(), key);
            assertEquals("id", descriptor.valuePropertyPath(), key);
            assertFalse(descriptor.labelPropertyPath().isBlank(), key);
            assertFalse(descriptor.entityLookup().searchPropertyPaths().isEmpty(), key);
            assertNotNull(descriptor.entityLookup().capabilities(), key);
            assertTrue(descriptor.entityLookup().capabilities().filter(), key);
            assertTrue(descriptor.entityLookup().capabilities().byIds(), key);
        });
    }

    @Test
    void shouldResolvePublishedNamedEndpointsToResourceEntitySchemaMetadata() throws Exception {
        for (SchemaLookupCase lookupCase : SCHEMA_LOOKUP_CASES.values()) {
            JsonNode schema = body(restTemplate.getForEntity(
                    "/schemas/filtered?path={path}&operation=post&schemaType=request",
                    String.class,
                    lookupCase.path()
            ));

            JsonNode fieldUi = schema.path("properties").path(lookupCase.fieldName()).path("x-ui");
            JsonNode optionSource = fieldUi.path("optionSource");

            assertEquals("entityLookup", fieldUi.path("controlType").asText(), lookupCase.toString());
            assertEquals(lookupCase.endpoint(), fieldUi.path("endpoint").asText(), lookupCase.toString());
            assertEquals(lookupCase.sourceKey(), optionSource.path("key").asText(), lookupCase.toString());
            assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText(), lookupCase.toString());
            assertEquals(lookupCase.resourcePath(), optionSource.path("resourcePath").asText(), lookupCase.toString());
            assertEquals(lookupCase.sourceKey(), optionSource.path("entityKey").asText(), lookupCase.toString());
            assertEquals("id", optionSource.path("valuePropertyPath").asText(), lookupCase.toString());
            assertTrue(optionSource.path("capabilities").path("filter").asBoolean(), lookupCase.toString());
            assertTrue(optionSource.path("capabilities").path("byIds").asBoolean(), lookupCase.toString());
            assertEquals(lookupCase.endpoint(), optionSource.path("filterEndpoint").asText(), lookupCase.toString());
            assertEquals(
                    lookupCase.resourcePath() + "/option-sources/" + lookupCase.sourceKey() + "/options/by-ids",
                    optionSource.path("byIdsEndpoint").asText(),
                    lookupCase.toString()
            );
            assertEquals("required", optionSource.path("selectedReloadPolicy").asText(), lookupCase.toString());
            assertEquals("reject", optionSource.path("invalidSortPolicy").asText(), lookupCase.toString());
        }
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private record SchemaLookupCase(
            String path,
            String fieldName,
            String endpoint,
            String sourceKey,
            String resourcePath
    ) {}
}
