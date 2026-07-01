package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.procurement.options.ExternalCatalogOptionSourceProvider;
import com.example.praxis.apiquickstart.procurement.options.ProcurementPaymentTermsOptionSourceProvider;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.service.OptionSourceExecutionRequest;
import org.praxisplatform.uischema.options.service.OptionSourceProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_external_option_source_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_external_option_source_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class ProcurementExternalOptionSourceProviderIntegrationTest {

    private static final String PAYMENT_TERMS_BY_IDS =
            ApiPaths.Procurement.SUPPLIERS + "/option-sources/"
                    + ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE
                    + "/options/by-ids";
    private static final String EXTERNAL_LOOKUP_BY_IDS =
            ApiPaths.Procurement.SUPPLIERS + "/option-sources/"
                    + ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE
                    + "/options/by-ids";

    @jakarta.annotation.Resource
    private TestRestTemplate restTemplate;

    @jakarta.annotation.Resource
    private ObjectMapper objectMapper;

    @jakarta.annotation.Resource
    private JwtTokenService jwtTokenService;

    @jakarta.annotation.Resource
    private List<OptionSourceProvider> optionSourceProviders;

    @jakarta.annotation.Resource
    private OptionSourceRegistry optionSourceRegistry;

    @SpyBean
    private ProcurementPaymentTermsOptionSourceProvider paymentTermsProvider;

    @SpyBean
    private ExternalCatalogOptionSourceProvider externalCatalogProvider;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void hostProviderExecutesFilterAndByIdsThroughCanonicalOptionSourceEndpoints() throws Exception {
        reset(paymentTermsProvider);
        assertTrue(
                optionSourceProviders.stream().anyMatch(ProcurementPaymentTermsOptionSourceProvider.class::isInstance),
                "The host-specific option source provider must be registered in the Spring context."
        );

        JsonNode filtered = ok(restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS + "?search=Net&page=0&size=10",
                authorizedJson("""
                        {
                          "companyId": 1
                        }
                        """),
                String.class
        ));

        assertEquals(2, filtered.path("content").size());
        assertEquals("NET30", filtered.path("content").get(0).path("id").asText());
        assertEquals("Pagamento em 30 dias", filtered.path("content").get(0).path("label").asText());
        assertTrue(filtered.path("content").get(0).path("extra").path("provider").isMissingNode());
        assertEquals(30, filtered.path("content").get(0).path("extra").path("settlementDays").asInt());
        assertFalse(filtered.toString().contains("sql"));
        assertFalse(filtered.toString().contains("providerConfig"));
        verify(paymentTermsProvider, atLeastOnce()).supports(any(), any(), any());
        verify(paymentTermsProvider).filter(any(OptionSourceExecutionRequest.class));

        JsonNode scopedOut = ok(restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS + "?search=Net&page=0&size=10",
                authorizedJson("""
                        {
                          "companyId": 99
                        }
                        """),
                String.class
        ));
        assertEquals(0, scopedOut.path("content").size());

        JsonNode byIds = ok(restTemplate.getForEntity(
                PAYMENT_TERMS_BY_IDS + "?ids=NET60&ids=NET30",
                String.class
        ));

        assertEquals("NET60", byIds.get(0).path("id").asText());
        assertEquals("NET30", byIds.get(1).path("id").asText());
        verify(paymentTermsProvider).byIds(any(OptionSourceExecutionRequest.class));
    }

    @Test
    void neutralExternalLookupProviderExecutesFilterDependenciesAndStringByIdsWithoutLeakingPrivateDetails() throws Exception {
        reset(externalCatalogProvider);
        assertTrue(
                optionSourceProviders.stream().anyMatch(ExternalCatalogOptionSourceProvider.class::isInstance),
                "The neutral external lookup provider must be registered in the Spring context."
        );

        JsonNode filtered = ok(restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_OPTIONS + "?search=External&page=0&size=10",
                authorizedJson("""
                        {
                          "companyId": 2
                        }
                        """),
                String.class
        ));

        assertEquals(2, filtered.path("content").size());
        assertEquals("EXT-CAT-A", filtered.path("content").get(0).path("id").asText());
        assertEquals("EXT-CAT-C", filtered.path("content").get(1).path("id").asText());
        assertEquals("general", filtered.path("content").get(0).path("extra").path("category").asText());
        assertTrue(filtered.path("content").get(0).path("extra").path("provider").isMissingNode());
        assertNoPrivateProviderDetails(filtered.toString());
        verify(externalCatalogProvider, atLeastOnce()).supports(any(), any(), any());
        verify(externalCatalogProvider).filter(any(OptionSourceExecutionRequest.class));

        JsonNode byIds = ok(restTemplate.getForEntity(
                EXTERNAL_LOOKUP_BY_IDS + "?ids=EXT-CAT-C&ids=EXT-CAT-A",
                String.class
        ));

        assertEquals("EXT-CAT-C", byIds.get(0).path("id").asText());
        assertEquals("EXT-CAT-A", byIds.get(1).path("id").asText());
        assertNoPrivateProviderDetails(byIds.toString());
        verify(externalCatalogProvider).byIds(any(OptionSourceExecutionRequest.class));
    }

    @Test
    void invalidPolicyRequestsAreRejectedBeforeHostProviderResolution() {
        reset(paymentTermsProvider);

        ResponseEntity<String> shortSearch = restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS + "?search=Ne&page=0&size=10",
                authorizedJson("{}"),
                String.class
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, shortSearch.getStatusCode(), shortSearch.getBody());
        verify(paymentTermsProvider, never()).supports(any(), any(), any());
        verify(paymentTermsProvider, never()).filter(any(OptionSourceExecutionRequest.class));
        assertNoPrivateProviderDetails(shortSearch.getBody());

        reset(paymentTermsProvider);
        ResponseEntity<String> oversizedPage = restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS + "?search=Net&page=0&size=21",
                authorizedJson("{}"),
                String.class
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, oversizedPage.getStatusCode(), oversizedPage.getBody());
        verify(paymentTermsProvider, never()).supports(any(), any(), any());
        verify(paymentTermsProvider, never()).filter(any(OptionSourceExecutionRequest.class));
        assertNoPrivateProviderDetails(oversizedPage.getBody());
    }

    @Test
    void includeIdsIsRejectedForPaymentTermsFilterBeforeHostProviderResolution() {
        reset(paymentTermsProvider);

        ResponseEntity<String> response = restTemplate.postForEntity(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS + "?includeIds=NET30&page=0&size=10",
                authorizedJson("{}"),
                String.class
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode(), response.getBody());
        assertTrue(response.getBody().contains("includeIds is not allowed"));
        verify(paymentTermsProvider, never()).supports(any(), any(), any());
        verify(paymentTermsProvider, never()).filter(any(OptionSourceExecutionRequest.class));
        assertNoPrivateProviderDetails(response.getBody());
    }

    @Test
    void hostProviderPublishesOnlyPublicDescriptorAndOpenApiEndpointMetadata() throws Exception {
        OptionSourceDescriptor descriptor = optionSourceRegistry
                .resolveByResourcePathAndKey(
                        ApiPaths.Procurement.SUPPLIERS,
                        ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE
                )
                .orElseThrow();
        Map<String, Object> metadata = descriptor.toMetadataMap();

        assertEquals(ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE, metadata.get("key"));
        assertEquals("LIGHT_LOOKUP", metadata.get("type"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS, metadata.get("resourcePath"));
        assertEquals("id", metadata.get("valuePropertyPath"));
        assertEquals("label", metadata.get("labelPropertyPath"));
        assertEquals(List.of("companyId"), metadata.get("dependsOn"));
        assertEquals(Map.of("companyId", "companyId"), metadata.get("dependencyFilterMap"));
        assertEquals(false, metadata.get("includeIds"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS, metadata.get("filterEndpoint"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS
                + "/option-sources/"
                + ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE
                + "/options/by-ids", metadata.get("byIdsEndpoint"));
        assertEquals("required", metadata.get("selectedReloadPolicy"));
        assertEquals("reject", metadata.get("invalidSortPolicy"));
        assertFalse(metadata.containsKey("providerConfig"));
        assertFalse(metadata.containsKey("hostContext"));
        assertFalse(metadata.containsKey("attributes"));
        assertFalse(metadata.containsKey("sql"));
        assertFalse(metadata.containsKey("function"));
        assertFalse(metadata.containsKey("package"));

        OptionSourceDescriptor externalDescriptor = optionSourceRegistry
                .resolveByResourcePathAndKey(
                        ApiPaths.Procurement.SUPPLIERS,
                        ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE
                )
                .orElseThrow();
        Map<String, Object> externalMetadata = externalDescriptor.toMetadataMap();

        assertEquals(ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE, externalMetadata.get("key"));
        assertEquals("LIGHT_LOOKUP", externalMetadata.get("type"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS, externalMetadata.get("resourcePath"));
        assertEquals("id", externalMetadata.get("valuePropertyPath"));
        assertEquals("label", externalMetadata.get("labelPropertyPath"));
        assertEquals(List.of("companyId"), externalMetadata.get("dependsOn"));
        assertEquals(Map.of("companyId", "companyId"), externalMetadata.get("dependencyFilterMap"));
        assertEquals(false, externalMetadata.get("includeIds"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_OPTIONS, externalMetadata.get("filterEndpoint"));
        assertEquals(ApiPaths.Procurement.SUPPLIERS
                + "/option-sources/"
                + ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE
                + "/options/by-ids", externalMetadata.get("byIdsEndpoint"));
        assertEquals("required", externalMetadata.get("selectedReloadPolicy"));
        assertEquals("reject", externalMetadata.get("invalidSortPolicy"));
        assertFalse(externalMetadata.containsKey("providerConfig"));
        assertFalse(externalMetadata.containsKey("hostContext"));
        assertFalse(externalMetadata.containsKey("attributes"));
        assertFalse(externalMetadata.containsKey("sql"));
        assertFalse(externalMetadata.containsKey("function"));
        assertFalse(externalMetadata.containsKey("package"));
        assertFalse(externalMetadata.containsKey("datasource"));
        assertFalse(externalMetadata.containsKey("bindParameters"));

        JsonNode openApi = ok(restTemplate.getForEntity("/v3/api-docs/api-procurement-suppliers", String.class));
        String openApiText = openApi.toString();
        assertTrue(openApi.path("paths")
                .has(ApiPaths.Procurement.SUPPLIERS + "/option-sources/{sourceKey}/options/filter"));
        assertTrue(openApi.path("paths")
                .has(ApiPaths.Procurement.SUPPLIERS + "/option-sources/{sourceKey}/options/by-ids"));
        assertTrue(openApiText.contains("OptionSourceFilterRequest"));
        assertTrue(openApiText.contains("PageOptionDTOObject"));
        assertTrue(openApiText.contains("OptionDTOObject"));
        assertNoPrivateProviderDetails(openApiText);
        assertFalse(openApiText.contains("ProcurementPaymentTermsOptionSourceProvider"));
        assertFalse(openApiText.contains("ExternalCatalogOptionSourceProvider"));
        assertFalse(openApiText.contains("OptionSourceExecutionContext"));
        assertFalse(openApiText.contains("hostContext"));
        assertFalse(openApiText.contains("attributes"));

        JsonNode requestSchema = ok(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=request",
                String.class,
                ApiPaths.Procurement.SUPPLIERS + "/option-sources/{sourceKey}/options/filter"
        ));
        JsonNode responseSchema = ok(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                String.class,
                ApiPaths.Procurement.SUPPLIERS + "/option-sources/{sourceKey}/options/filter"
        ));
        assertTrue(requestSchema.path("properties").has("filter"), requestSchema.toPrettyString());
        assertTrue(responseSchema.toString().contains("OptionDTO"));
        assertNoPrivateProviderDetails(requestSchema.toString());
        assertNoPrivateProviderDetails(responseSchema.toString());
    }

    private JsonNode ok(ResponseEntity<String> response) throws Exception {
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

    private void assertNoPrivateProviderDetails(String body) {
        String value = body == null ? "" : body;
        assertFalse(value.contains("providerConfig"));
        assertFalse(value.contains("\"provider\""));
        assertFalse(value.contains("\"providerName\""));
        assertFalse(value.contains("\"package\""));
        assertFalse(value.contains("\"function\""));
        assertFalse(value.contains("\"table\""));
        assertFalse(value.contains("\"datasource\""));
        assertFalse(value.contains("\"bindParameters\""));
        assertFalse(value.contains("\"hostContext\""));
        assertFalse(value.contains("\"attributes\""));
        assertFalse(value.contains("select * from"));
        assertFalse(value.contains("internal-token"));
        assertFalse(value.contains("private-endpoint"));
    }
}
