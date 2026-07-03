package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_procurement_entity_lookup_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_procurement_entity_lookup_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class ProcurementEntityLookupPilotIntegrationTest {

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

    @MockBean
    private DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver;

    @MockBean
    private DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver;

    @BeforeEach
    void seedProcurementTables() {
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());
        when(backendValidationPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());
        when(optionSourcePolicyResolver.resolveAppliedSelectionPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.procurement_purchase_orders");
        jdbcTemplate.execute("drop table if exists public.procurement_products");
        jdbcTemplate.execute("drop table if exists public.procurement_contracts");
        jdbcTemplate.execute("drop table if exists public.procurement_suppliers");
        jdbcTemplate.execute("drop table if exists public.procurement_companies");

        jdbcTemplate.execute("""
                create table public.procurement_companies (
                    id integer primary key,
                    code varchar(40) not null,
                    legal_name varchar(200) not null,
                    document_number varchar(40),
                    city varchar(120),
                    state varchar(40),
                    status varchar(40),
                    disabled_reason varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                create table public.procurement_suppliers (
                    id integer primary key,
                    company_id integer not null,
                    code varchar(40) not null,
                    legal_name varchar(200) not null,
                    document_number varchar(40),
                    homologation_status varchar(40),
                    risk_level varchar(40),
                    status varchar(40),
                    disabled_reason varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                create table public.procurement_contracts (
                    id integer primary key,
                    company_id integer not null,
                    supplier_id integer not null,
                    number varchar(80) not null,
                    supplier_name varchar(200),
                    currency varchar(10),
                    valid_until date,
                    status varchar(40),
                    disabled_reason varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                create table public.procurement_products (
                    id integer primary key,
                    company_id integer not null,
                    contract_id integer,
                    sku varchar(80) not null,
                    name varchar(200) not null,
                    category_name varchar(120),
                    stock_available integer,
                    unit_of_measure varchar(20),
                    status varchar(40),
                    disabled_reason varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                create table public.procurement_purchase_orders (
                    id integer primary key,
                    company_id integer not null,
                    supplier_id integer not null,
                    contract_id integer,
                    product_id integer not null,
                    order_date date,
                    currency varchar(10),
                    quantity integer
                )
                """);

        jdbcTemplate.update("""
                insert into public.procurement_companies
                    (id, code, legal_name, document_number, city, state, status, disabled_reason)
                values
                    (1, 'EMP-001', 'Praxis Brasil Ltda', '11.222.333/0001-44', 'Sao Paulo', 'SP', 'ACTIVE', null),
                    (2, 'EMP-003', 'Praxis Nordeste', '11.222.333/0003-06', 'Recife', 'PE', 'INACTIVE', 'Empresa inativa desde 01/2026')
                """);
        jdbcTemplate.update("""
                insert into public.procurement_suppliers
                    (id, company_id, code, legal_name, document_number, homologation_status, risk_level, status, disabled_reason)
                values
                    (10, 1, 'FOR-8742', 'ACME Suprimentos Ltda', '12.345.678/0001-90', 'APPROVED', 'LOW', 'ACTIVE', null),
                    (11, 1, 'FOR-9921', 'Fornecedor Bloqueado S.A.', '98.765.432/0001-10', 'SUSPENDED', 'HIGH', 'BLOCKED', 'Fornecedor bloqueado para novos pedidos'),
                    (12, 2, 'FOR-1000', 'ACME Nordeste', '55.000.111/0001-00', 'APPROVED', 'LOW', 'ACTIVE', null)
                """);
        jdbcTemplate.update("""
                insert into public.procurement_contracts
                    (id, company_id, supplier_id, number, supplier_name, currency, valid_until, status, disabled_reason)
                values
                    (20, 1, 10, 'CTR-2026-001', 'ACME Suprimentos Ltda', 'BRL', DATE '2026-12-31', 'SIGNED', null),
                    (21, 1, 10, 'CTR-2025-009', 'ACME Suprimentos Ltda', 'BRL', DATE '2025-12-31', 'EXPIRED', 'Contrato expirado em 31/12/2025'),
                    (22, 1, 10, 'CTR-2026-002', 'ACME Suprimentos Ltda', 'BRL', DATE '2026-11-30', 'DRAFT', null)
                """);
        jdbcTemplate.update("""
                insert into public.procurement_products
                    (id, company_id, contract_id, sku, name, category_name, stock_available, unit_of_measure, status, disabled_reason)
                values
                    (30, 1, 20, 'PRD-00123', 'Notebook Dell Latitude', 'Equipamentos', 12, 'UN', 'ACTIVE', null),
                    (31, 1, 20, 'PRD-00444', 'Docking Station Legacy', 'Equipamentos', 0, 'UN', 'BLOCKED', 'Produto sem estoque liberado')
                """);
    }

    @Test
    void shouldExposeEntityLookupMetadataAndExecuteGovernedLookups() throws Exception {
        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.Procurement.PURCHASE_ORDERS
        ));

        JsonNode supplierLookup = schema.path("properties").path("supplierId").path("x-ui").path("optionSource");
        assertEquals("entityLookup", schema.path("properties").path("supplierId").path("x-ui").path("controlType").asText());
        assertEquals(ApiPaths.Procurement.COMPANIES,
                schema.path("properties").path("companyId").path("x-ui").path("optionSource").path("resourcePath").asText());
        assertEquals(ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE, supplierLookup.path("key").asText());
        assertEquals("RESOURCE_ENTITY", supplierLookup.path("type").asText());
        assertEquals(ApiPaths.Procurement.SUPPLIERS, supplierLookup.path("resourcePath").asText());
        assertEquals(ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE, supplierLookup.path("entityKey").asText());
        assertEquals("code", supplierLookup.path("codePropertyPath").asText());
        assertEquals("status", supplierLookup.path("statusPropertyPath").asText());
        assertEquals("companyId", supplierLookup.path("dependsOn").get(0).asText());
        assertEquals("companyId", supplierLookup.path("dependencyFilterMap").path("companyId").asText());
        assertTrue(supplierLookup.path("capabilities").path("byIds").asBoolean());
        assertTrue(supplierLookup.path("selectionPolicy").path("allowRetainInvalidExistingValue").asBoolean());
        assertEquals(ApiPaths.Procurement.CONTRACTS,
                schema.path("properties").path("contractId").path("x-ui").path("optionSource").path("resourcePath").asText());
        assertEquals(ApiPaths.Procurement.PRODUCTS,
                schema.path("properties").path("productId").path("x-ui").path("optionSource").path("resourcePath").asText());

        JsonNode suppliers = body(restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS + "?search=ACME",
                authorizedJson("""
                        {
                          "companyId": 1
                        }
                        """),
                String.class
        ));
        assertEquals(1, suppliers.path("content").size());
        JsonNode acme = suppliers.path("content").get(0);
        assertEquals(10, acme.path("id").asInt());
        assertEquals("ACME Suprimentos Ltda", acme.path("label").asText());
        assertEquals("FOR-8742", acme.path("extra").path("code").asText());
        assertEquals("ACTIVE", acme.path("extra").path("status").asText());
        assertTrue(acme.path("extra").path("selectable").asBoolean());

        JsonNode blockedSupplier = objectMapper.readTree(restTemplate.getForObject(
                "/api/procurement/suppliers/option-sources/supplier/options/by-ids?ids=11",
                String.class
        ));
        assertEquals("Fornecedor Bloqueado S.A.", blockedSupplier.get(0).path("label").asText());
        assertFalse(blockedSupplier.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Fornecedor bloqueado para novos pedidos", blockedSupplier.get(0).path("extra").path("disabledReason").asText());

        JsonNode contracts = body(restTemplate.postForEntity(
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS + "?search=CTR",
                authorizedJson("""
                        {
                          "companyId": 1,
                          "supplierId": 10
                        }
                        """),
                String.class
        ));
        assertEquals(3, contracts.path("content").size());
        JsonNode activeContract = findByLabel(contracts.path("content"), "CTR-2026-001");
        JsonNode expiredContract = findByLabel(contracts.path("content"), "CTR-2025-009");
        JsonNode draftContract = findByLabel(contracts.path("content"), "CTR-2026-002");
        assertNotNull(activeContract);
        assertNotNull(expiredContract);
        assertNotNull(draftContract);
        assertTrue(activeContract.path("extra").path("selectable").asBoolean());
        assertFalse(expiredContract.path("extra").path("selectable").asBoolean());
        assertFalse(draftContract.path("extra").path("selectable").asBoolean());
    }

    @Test
    void shouldExposeProcurementEntityLookupMetadataAcrossFormsAndFilters() throws Exception {
        assertProcurementLookup(
                ApiPaths.Procurement.SUPPLIERS,
                "companyId",
                "entityLookup",
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        );
        assertProcurementLookup(
                ApiPaths.Procurement.CONTRACTS,
                "companyId",
                "entityLookup",
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        );
        assertProcurementLookup(
                ApiPaths.Procurement.CONTRACTS,
                "supplierId",
                "entityLookup",
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS,
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS
        );
        assertProcurementLookup(
                ApiPaths.Procurement.PRODUCTS,
                "companyId",
                "entityLookup",
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        );
        assertProcurementLookup(
                ApiPaths.Procurement.PRODUCTS,
                "contractId",
                "entityLookup",
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS,
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                ApiPaths.Procurement.CONTRACTS
        );

        assertProcurementLookup(
                ApiPaths.Procurement.SUPPLIERS + "/filter",
                "companyId",
                "inlineEntityLookup",
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
                ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                ApiPaths.Procurement.COMPANIES
        );
        assertProcurementLookup(
                ApiPaths.Procurement.CONTRACTS + "/filter",
                "supplierId",
                "inlineEntityLookup",
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS,
                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS
        );
        assertProcurementLookup(
                ApiPaths.Procurement.PRODUCTS + "/filter",
                "contractId",
                "inlineEntityLookup",
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_OPTIONS,
                ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                ApiPaths.Procurement.CONTRACTS
        );
        assertProcurementLookup(
                ApiPaths.Procurement.PURCHASE_ORDERS + "/filter",
                "productId",
                "inlineEntityLookup",
                ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_OPTIONS,
                ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_SOURCE,
                ApiPaths.Procurement.PRODUCTS
        );
    }

    @Test
    void shouldExposeProcurementCockpitSurfaces() throws Exception {
        JsonNode supplierSurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=procurement.suppliers",
                String.class
        ));
        assertEquals("procurement.suppliers", supplierSurfaces.path("resourceKey").asText());
        JsonNode homologationBoard = findById(supplierSurfaces.path("surfaces"), "supplier-homologation-board");
        assertNotNull(homologationBoard);
        assertEquals("VIEW", homologationBoard.path("kind").asText());
        assertEquals("COLLECTION", homologationBoard.path("scope").asText());

        JsonNode contractSurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=procurement.contracts",
                String.class
        ));
        JsonNode contractBoard = findById(contractSurfaces.path("surfaces"), "contract-governance-board");
        assertNotNull(contractBoard);
        assertEquals("VIEW", contractBoard.path("kind").asText());
        assertEquals("COLLECTION", contractBoard.path("scope").asText());

        JsonNode purchaseOrderSurfaces = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=procurement.purchase-orders",
                String.class
        ));
        assertEquals("procurement.purchase-orders", purchaseOrderSurfaces.path("resourceKey").asText());
        assertNotNull(findById(purchaseOrderSurfaces.path("surfaces"), "purchase-order-control-board"));

        JsonNode issueSurface = findById(purchaseOrderSurfaces.path("surfaces"), "issue-purchase-order");
        assertNotNull(issueSurface);
        assertEquals("FORM", issueSurface.path("kind").asText());
        assertEquals("COLLECTION", issueSurface.path("scope").asText());
    }

    @Test
    void shouldExposeAndExecuteContractLifecycleWorkflowActions() throws Exception {
        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=procurement.contracts",
                String.class
        ));
        assertEquals("procurement.contracts", actionsCatalog.path("resourceKey").asText());
        JsonNode sign = findById(actionsCatalog.path("actions"), "sign");
        JsonNode suspend = findById(actionsCatalog.path("actions"), "suspend");
        JsonNode reactivate = findById(actionsCatalog.path("actions"), "reactivate");
        assertNotNull(sign);
        assertNotNull(suspend);
        assertNotNull(reactivate);
        assertEquals("ITEM", sign.path("scope").asText());
        assertEquals("/api/procurement/contracts/{id}/actions/sign", sign.path("path").asText());

        ResponseEntity<String> signResponse = restTemplate.exchange(
                "/api/procurement/contracts/22/actions/sign",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Contrato revisado pelo time juridico"
                        }
                        """),
                String.class
        );
        JsonNode signed = body(signResponse);
        assertEquals("DRAFT", signed.path("data").path("statusAnterior").asText());
        assertEquals("SIGNED", signed.path("data").path("statusAtual").asText());

        ResponseEntity<String> suspendResponse = restTemplate.exchange(
                "/api/procurement/contracts/20/actions/suspend",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Auditoria de fornecedor pendente"
                        }
                        """),
                String.class
        );
        JsonNode suspended = body(suspendResponse);
        assertEquals("SIGNED", suspended.path("data").path("statusAnterior").asText());
        assertEquals("SUSPENDED", suspended.path("data").path("statusAtual").asText());

        JsonNode suspendedContract = objectMapper.readTree(restTemplate.getForObject(
                "/api/procurement/contracts/option-sources/contract/options/by-ids?ids=20",
                String.class
        ));
        assertFalse(suspendedContract.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Auditoria de fornecedor pendente", suspendedContract.get(0).path("extra").path("disabledReason").asText());

        ResponseEntity<String> duplicateSuspend = restTemplate.exchange(
                "/api/procurement/contracts/20/actions/suspend",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Tentativa duplicada"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateSuspend.getStatusCode());

        ResponseEntity<String> reactivateResponse = restTemplate.exchange(
                "/api/procurement/contracts/20/actions/reactivate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Auditoria concluida"
                        }
                        """),
                String.class
        );
        JsonNode reactivated = body(reactivateResponse);
        assertEquals("SUSPENDED", reactivated.path("data").path("statusAnterior").asText());
        assertEquals("SIGNED", reactivated.path("data").path("statusAtual").asText());

        JsonNode activeContract = objectMapper.readTree(restTemplate.getForObject(
                "/api/procurement/contracts/option-sources/contract/options/by-ids?ids=20",
                String.class
        ));
        assertTrue(activeContract.get(0).path("extra").path("selectable").asBoolean());
        assertTrue(activeContract.get(0).path("extra").path("disabledReason").isMissingNode()
                || activeContract.get(0).path("extra").path("disabledReason").isNull()
                || activeContract.get(0).path("extra").path("disabledReason").asText().isBlank());
    }

    @Test
    void shouldExposeAndExecuteSupplierEligibilityWorkflowActions() throws Exception {
        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=procurement.suppliers",
                String.class
        ));
        assertEquals("procurement.suppliers", actionsCatalog.path("resourceKey").asText());
        JsonNode block = findById(actionsCatalog.path("actions"), "block");
        JsonNode reinstate = findById(actionsCatalog.path("actions"), "reinstate");
        assertNotNull(block);
        assertNotNull(reinstate);
        assertEquals("ITEM", block.path("scope").asText());
        assertEquals("/api/procurement/suppliers/{id}/actions/block", block.path("path").asText());

        ResponseEntity<String> blockResponse = restTemplate.exchange(
                "/api/procurement/suppliers/10/actions/block",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Risco de compliance confirmado"
                        }
                        """),
                String.class
        );
        JsonNode blocked = body(blockResponse);
        assertEquals("ACTIVE", blocked.path("data").path("statusAnterior").asText());
        assertEquals("BLOCKED", blocked.path("data").path("statusAtual").asText());

        JsonNode blockedSupplier = objectMapper.readTree(restTemplate.getForObject(
                "/api/procurement/suppliers/option-sources/supplier/options/by-ids?ids=10",
                String.class
        ));
        assertFalse(blockedSupplier.get(0).path("extra").path("selectable").asBoolean());
        assertEquals("Risco de compliance confirmado", blockedSupplier.get(0).path("extra").path("disabledReason").asText());

        ResponseEntity<String> duplicateBlock = restTemplate.exchange(
                "/api/procurement/suppliers/10/actions/block",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Tentativa duplicada"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateBlock.getStatusCode());

        ResponseEntity<String> reinstateResponse = restTemplate.exchange(
                "/api/procurement/suppliers/10/actions/reinstate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Fornecedor reavaliado e liberado"
                        }
                        """),
                String.class
        );
        JsonNode reinstated = body(reinstateResponse);
        assertEquals("BLOCKED", reinstated.path("data").path("statusAnterior").asText());
        assertEquals("ACTIVE", reinstated.path("data").path("statusAtual").asText());

        JsonNode activeSupplier = objectMapper.readTree(restTemplate.getForObject(
                "/api/procurement/suppliers/option-sources/supplier/options/by-ids?ids=10",
                String.class
        ));
        assertTrue(activeSupplier.get(0).path("extra").path("selectable").asBoolean());
        assertTrue(activeSupplier.get(0).path("extra").path("disabledReason").isMissingNode()
                || activeSupplier.get(0).path("extra").path("disabledReason").isNull()
                || activeSupplier.get(0).path("extra").path("disabledReason").asText().isBlank());
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

    private void assertProcurementLookup(
            String path,
            String fieldName,
            String expectedControlType,
            String expectedEndpoint,
            String expectedKey,
            String expectedResourcePath
    ) throws Exception {
        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                path
        ));

        JsonNode fieldUi = schema.path("properties").path(fieldName).path("x-ui");
        JsonNode optionSource = fieldUi.path("optionSource");
        assertEquals(expectedControlType, fieldUi.path("controlType").asText(), path + "#" + fieldName);
        assertEquals(expectedEndpoint, fieldUi.path("endpoint").asText(), path + "#" + fieldName);
        assertEquals(expectedKey, optionSource.path("key").asText(), path + "#" + fieldName);
        assertEquals("RESOURCE_ENTITY", optionSource.path("type").asText(), path + "#" + fieldName);
        assertEquals(expectedResourcePath, optionSource.path("resourcePath").asText(), path + "#" + fieldName);
        assertEquals(expectedKey, optionSource.path("entityKey").asText(), path + "#" + fieldName);
        assertEquals("id", optionSource.path("valuePropertyPath").asText(), path + "#" + fieldName);
        assertTrue(optionSource.path("capabilities").path("filter").asBoolean(), path + "#" + fieldName);
        assertTrue(optionSource.path("capabilities").path("byIds").asBoolean(), path + "#" + fieldName);
    }

    private JsonNode findByLabel(JsonNode items, String label) {
        for (JsonNode item : items) {
            if (label.equals(item.path("label").asText())) {
                return item;
            }
        }
        return null;
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
