package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_assets_lookup_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_assets_lookup_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class OperationalAssetsEntityLookupPilotIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @MockBean
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @BeforeEach
    void seedTables() {
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.equipamentos");
        jdbcTemplate.execute("drop table if exists public.veiculos");
        jdbcTemplate.execute("drop table if exists public.funcionarios");
        jdbcTemplate.execute("""
                create table public.funcionarios (
                    id integer primary key,
                    nome_completo varchar(200),
                    cpf varchar(20),
                    email varchar(200),
                    telefone varchar(40),
                    data_nascimento date,
                    cidade_nascimento varchar(120),
                    pais_nascimento varchar(120),
                    estado_civil varchar(40),
                    data_admissao date,
                    salario numeric(19, 2),
                    foto_perfil_url varchar(500),
                    ativo boolean,
                    cargo_id integer,
                    departamento_id integer
                )
                """);
        jdbcTemplate.execute("""
                create table public.equipamentos (
                    id integer primary key,
                    nome varchar(200) not null,
                    tipo varchar(40),
                    resistencia integer,
                    proprietario_id integer,
                    status varchar(40) not null
                )
                """);
        jdbcTemplate.execute("""
                create table public.veiculos (
                    id integer primary key,
                    nome varchar(200) not null,
                    tipo varchar(40),
                    capacidade integer,
                    proprietario_id integer,
                    status varchar(40) not null
                )
                """);

        jdbcTemplate.update("""
                insert into public.veiculos (id, nome, tipo, capacidade, proprietario_id, status)
                values (1, 'Invisible Jet', 'AEREO', 6, null, 'OPERACIONAL')
                """);
        jdbcTemplate.update("""
                insert into public.veiculos (id, nome, tipo, capacidade, proprietario_id, status)
                values (2, 'Damaged Rover', 'TERRESTRE', 4, null, 'MANUTENCAO')
                """);
        jdbcTemplate.update("""
                insert into public.equipamentos (id, nome, tipo, resistencia, proprietario_id, status)
                values (1, 'Vibranium Shield', 'ARTEFATO', 95, null, 'ESTOQUE')
                """);
        jdbcTemplate.update("""
                insert into public.equipamentos (id, nome, tipo, resistencia, proprietario_id, status)
                values (2, 'Cracked Grapple', 'GADGET', 20, null, 'QUEBRADO')
                """);
    }

    @Test
    void shouldExposeVehicleResourceEntityLookupForMissionUsage() throws Exception {
        JsonNode usageSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/veiculo-missao-usos&operation=post&schemaType=request",
                String.class
        ));

        JsonNode vehicleUi = usageSchema.path("properties").path("veiculoId").path("x-ui");
        JsonNode optionSource = vehicleUi.path("optionSource");
        assertEquals("entityLookup", vehicleUi.path("controlType").asText());
        assertEquals("/api/assets/veiculos/option-sources/vehicle/options/filter",
                vehicleUi.path("endpoint").asText());
        assertEquals("vehicle", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/assets/veiculos", optionSource.path("resourcePath").asText());
        assertEquals("veiculoId", optionSource.path("filterField").asText());
        assertEquals("vehicle", optionSource.path("entityKey").asText());
        assertEquals("nome", optionSource.path("labelPropertyPath").asText());
        assertEquals("status", optionSource.path("statusPropertyPath").asText());
        assertEquals("tipo", optionSource.path("descriptionPropertyPaths").get(0).asText());
        assertEquals("capacidade", optionSource.path("descriptionPropertyPaths").get(1).asText());
        assertEquals("nome", optionSource.path("searchPropertyPaths").get(0).asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
        assertTrue(optionSource.path("selectionPolicy").path("allowedStatuses").toString().contains("OPERACIONAL"));
        assertTrue(optionSource.path("selectionPolicy").path("blockedStatuses").toString().contains("MANUTENCAO"));

        JsonNode usageFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/veiculo-missao-usos/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("inlineEntityLookup",
                usageFilterSchema.path("properties").path("veiculoId").path("x-ui").path("controlType").asText());
        assertEquals("vehicle",
                usageFilterSchema.path("properties").path("veiculoId").path("x-ui").path("optionSource").path("key").asText());
        assertEmployeeLookup(
                usageSchema.path("properties").path("pilotoId").path("x-ui"),
                "entityLookup"
        );
        assertEmployeeLookup(
                usageFilterSchema.path("properties").path("pilotoId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode vehicles = body(restTemplate.postForEntity(
                "/api/assets/veiculos/option-sources/vehicle/options/filter?search=Invisible",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, vehicles.path("content").size());
        JsonNode invisibleJet = vehicles.path("content").get(0);
        assertEquals(1, invisibleJet.path("id").asInt());
        assertEquals("Invisible Jet", invisibleJet.path("label").asText());
        assertEquals("AEREO - 6", invisibleJet.path("extra").path("description").asText());
        assertEquals("OPERACIONAL", invisibleJet.path("extra").path("status").asText());
        assertTrue(invisibleJet.path("extra").path("selectable").asBoolean());

        JsonNode selectedVehicles = objectMapper.readTree(restTemplate.getForObject(
                "/api/assets/veiculos/option-sources/vehicle/options/by-ids?ids=1&ids=2",
                String.class
        ));
        assertEquals(2, selectedVehicles.size());
        assertEquals("Invisible Jet", selectedVehicles.get(0).path("label").asText());
        assertTrue(selectedVehicles.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Damaged Rover", selectedVehicles.get(1).path("label").asText());
        assertEquals("MANUTENCAO", selectedVehicles.get(1).path("extra").path("status").asText());
        assertFalse(selectedVehicles.get(1).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldExposeEquipmentResourceEntityLookupForAllocations() throws Exception {
        JsonNode allocationSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/equipamento-alocacoes&operation=post&schemaType=request",
                String.class
        ));

        JsonNode equipmentUi = allocationSchema.path("properties").path("equipamentoId").path("x-ui");
        JsonNode optionSource = equipmentUi.path("optionSource");
        assertEquals("entityLookup", equipmentUi.path("controlType").asText());
        assertEquals("/api/assets/equipamentos/option-sources/equipment/options/filter",
                equipmentUi.path("endpoint").asText());
        assertEquals("equipment", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/assets/equipamentos", optionSource.path("resourcePath").asText());
        assertEquals("equipamentoId", optionSource.path("filterField").asText());
        assertEquals("equipment", optionSource.path("entityKey").asText());
        assertEquals("nome", optionSource.path("labelPropertyPath").asText());
        assertEquals("status", optionSource.path("statusPropertyPath").asText());
        assertEquals("tipo", optionSource.path("descriptionPropertyPaths").get(0).asText());
        assertEquals("resistencia", optionSource.path("descriptionPropertyPaths").get(1).asText());
        assertEquals("nome", optionSource.path("searchPropertyPaths").get(0).asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
        assertTrue(optionSource.path("selectionPolicy").path("allowedStatuses").toString().contains("ESTOQUE"));
        assertTrue(optionSource.path("selectionPolicy").path("blockedStatuses").toString().contains("QUEBRADO"));

        JsonNode allocationFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/equipamento-alocacoes/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEquals("inlineEntityLookup",
                allocationFilterSchema.path("properties").path("equipamentoId").path("x-ui").path("controlType").asText());
        assertEquals("equipment",
                allocationFilterSchema.path("properties").path("equipamentoId").path("x-ui").path("optionSource").path("key").asText());
        assertEmployeeLookup(
                allocationSchema.path("properties").path("funcionarioId").path("x-ui"),
                "entityLookup"
        );
        assertEmployeeLookup(
                allocationFilterSchema.path("properties").path("funcionarioId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode equipment = body(restTemplate.postForEntity(
                "/api/assets/equipamentos/option-sources/equipment/options/filter?search=Vibranium",
                authorizedJson("{}"),
                String.class
        ));
        assertEquals(1, equipment.path("content").size());
        JsonNode shield = equipment.path("content").get(0);
        assertEquals(1, shield.path("id").asInt());
        assertEquals("Vibranium Shield", shield.path("label").asText());
        assertEquals("ARTEFATO - 95", shield.path("extra").path("description").asText());
        assertEquals("ESTOQUE", shield.path("extra").path("status").asText());
        assertTrue(shield.path("extra").path("selectable").asBoolean());

        JsonNode selectedEquipment = objectMapper.readTree(restTemplate.getForObject(
                "/api/assets/equipamentos/option-sources/equipment/options/by-ids?ids=1&ids=2",
                String.class
        ));
        assertEquals(2, selectedEquipment.size());
        assertEquals("Vibranium Shield", selectedEquipment.get(0).path("label").asText());
        assertTrue(selectedEquipment.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Cracked Grapple", selectedEquipment.get(1).path("label").asText());
        assertEquals("QUEBRADO", selectedEquipment.get(1).path("extra").path("status").asText());
        assertFalse(selectedEquipment.get(1).path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldReuseEmployeeResourceEntityLookupForAssetOwnershipFilters() throws Exception {
        JsonNode vehicleSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/veiculos&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                vehicleSchema.path("properties").path("proprietarioId").path("x-ui"),
                "entityLookup"
        );

        JsonNode vehicleFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/veiculos/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                vehicleFilterSchema.path("properties").path("proprietarioId").path("x-ui"),
                "inlineEntityLookup"
        );

        JsonNode equipmentSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/equipamentos&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                equipmentSchema.path("properties").path("proprietarioId").path("x-ui"),
                "entityLookup"
        );

        JsonNode equipmentFilterSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/assets/equipamentos/filter&operation=post&schemaType=request",
                String.class
        ));
        assertEmployeeLookup(
                equipmentFilterSchema.path("properties").path("proprietarioId").path("x-ui"),
                "inlineEntityLookup"
        );
    }

    @Test
    void shouldExposeOperationalAssetsCockpitSurfaces() throws Exception {
        assertAssetSurface(
                "assets.equipamentos",
                "equipment-inventory-board",
                "Inventario de equipamentos"
        );
        assertAssetSurface(
                "assets.equipamento-alocacoes",
                "equipment-custody-board",
                "Cadeia de custodia"
        );
        assertAssetSurface(
                "assets.veiculos",
                "fleet-readiness-board",
                "Prontidao da frota"
        );
        assertAssetSurface(
                "assets.veiculo-missao-usos",
                "mission-fleet-usage-board",
                "Frota em missao"
        );
    }

    @Test
    void shouldExposeAndExecuteAssetAvailabilityWorkflowActions() throws Exception {
        JsonNode equipmentActions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=assets.equipamentos",
                String.class
        ));
        assertEquals("assets.equipamentos", equipmentActions.path("resourceKey").asText());
        assertAction(equipmentActions, "send-to-maintenance", "/api/assets/equipamentos/{id}/actions/send-to-maintenance");
        assertAction(equipmentActions, "return-to-stock", "/api/assets/equipamentos/{id}/actions/return-to-stock");

        ResponseEntity<String> maintenanceEquipmentResponse = restTemplate.exchange(
                "/api/assets/equipamentos/1/actions/send-to-maintenance",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Inspecao preventiva"
                        }
                        """),
                String.class
        );
        JsonNode maintenanceEquipment = body(maintenanceEquipmentResponse);
        assertEquals("ESTOQUE", maintenanceEquipment.path("data").path("statusAnterior").asText());
        assertEquals("MANUTENCAO", maintenanceEquipment.path("data").path("statusAtual").asText());
        assertEquipmentSelectable(1, false);

        ResponseEntity<String> duplicateEquipmentResponse = restTemplate.exchange(
                "/api/assets/equipamentos/1/actions/send-to-maintenance",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Tentativa duplicada"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateEquipmentResponse.getStatusCode());

        ResponseEntity<String> stockEquipmentResponse = restTemplate.exchange(
                "/api/assets/equipamentos/1/actions/return-to-stock",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Inspecao concluida"
                        }
                        """),
                String.class
        );
        JsonNode stockEquipment = body(stockEquipmentResponse);
        assertEquals("MANUTENCAO", stockEquipment.path("data").path("statusAnterior").asText());
        assertEquals("ESTOQUE", stockEquipment.path("data").path("statusAtual").asText());
        assertEquipmentSelectable(1, true);

        JsonNode vehicleActions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=assets.veiculos",
                String.class
        ));
        assertEquals("assets.veiculos", vehicleActions.path("resourceKey").asText());
        assertAction(vehicleActions, "send-to-maintenance", "/api/assets/veiculos/{id}/actions/send-to-maintenance");
        assertAction(vehicleActions, "return-to-operation", "/api/assets/veiculos/{id}/actions/return-to-operation");

        ResponseEntity<String> maintenanceVehicleResponse = restTemplate.exchange(
                "/api/assets/veiculos/1/actions/send-to-maintenance",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Revisao da frota"
                        }
                        """),
                String.class
        );
        JsonNode maintenanceVehicle = body(maintenanceVehicleResponse);
        assertEquals("OPERACIONAL", maintenanceVehicle.path("data").path("statusAnterior").asText());
        assertEquals("MANUTENCAO", maintenanceVehicle.path("data").path("statusAtual").asText());
        assertVehicleSelectable(1, false);

        ResponseEntity<String> operationalVehicleResponse = restTemplate.exchange(
                "/api/assets/veiculos/1/actions/return-to-operation",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Liberado pela manutencao"
                        }
                        """),
                String.class
        );
        JsonNode operationalVehicle = body(operationalVehicleResponse);
        assertEquals("MANUTENCAO", operationalVehicle.path("data").path("statusAnterior").asText());
        assertEquals("OPERACIONAL", operationalVehicle.path("data").path("statusAtual").asText());
        assertVehicleSelectable(1, true);
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }

    private void assertEmployeeLookup(JsonNode fieldUi, String expectedControlType) {
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(expectedControlType, fieldUi.path("controlType").asText());
        assertEquals("/api/human-resources/funcionarios/option-sources/employee/options/filter",
                fieldUi.path("endpoint").asText());
        assertEquals("employee", optionSource.path("key").asText());
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText());
        assertEquals("/api/human-resources/funcionarios", optionSource.path("resourcePath").asText());
        assertFalse(optionSource.hasNonNull("filterField"));
        assertEquals("employee", optionSource.path("entityKey").asText());
        assertEquals("nomeCompleto", optionSource.path("labelPropertyPath").asText());
        assertEquals("ativo", optionSource.path("selectionPolicy").path("selectablePropertyPath").asText());
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean());
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean());
    }

    private void assertAssetSurface(String resourceKey, String surfaceId, String title) throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource={resourceKey}",
                String.class,
                resourceKey
        ));
        assertEquals(resourceKey, surfacesCatalog.path("resourceKey").asText());
        JsonNode surface = findById(surfacesCatalog.path("surfaces"), surfaceId);
        assertNotNull(surface);
        assertEquals("VIEW", surface.path("kind").asText());
        assertEquals("COLLECTION", surface.path("scope").asText());
        assertEquals(title, surface.path("title").asText());
    }

    private void assertAction(JsonNode actionsCatalog, String actionId, String path) {
        JsonNode action = findById(actionsCatalog.path("actions"), actionId);
        assertNotNull(action);
        assertEquals("ITEM", action.path("scope").asText());
        assertEquals(path, action.path("path").asText());
    }

    private void assertEquipmentSelectable(int id, boolean expectedSelectable) throws Exception {
        JsonNode selectedEquipment = objectMapper.readTree(restTemplate.getForObject(
                "/api/assets/equipamentos/option-sources/equipment/options/by-ids?ids={id}",
                String.class,
                id
        ));
        assertEquals(expectedSelectable, selectedEquipment.get(0).path("extra").path("selectable").asBoolean());
    }

    private void assertVehicleSelectable(int id, boolean expectedSelectable) throws Exception {
        JsonNode selectedVehicle = objectMapper.readTree(restTemplate.getForObject(
                "/api/assets/veiculos/option-sources/vehicle/options/by-ids?ids={id}",
                String.class,
                id
        ));
        assertEquals(expectedSelectable, selectedVehicle.get(0).path("extra").path("selectable").asBoolean());
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText())) {
                return item;
            }
        }
        return null;
    }
}
