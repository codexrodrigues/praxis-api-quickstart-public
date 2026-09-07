package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.praxisplatform.config.repository.DomainRuleDefinitionApprovalRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Downstream proof of the governed palette lifecycle hosted by the Quickstart. */
@EnabledIfSystemProperty(named = "praxis.palette.proof", matches = "true")
@SpringBootTest(
    classes = ApiQuickstartApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.rate-limit.enabled=false",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.csrf.disable=true",
        "app.session.cookie-name=SESSION",
        "app.session.secure=false",
        "praxis.ai.provider=mock",
        "praxis.ai.security.corporate-mode=true",
        "praxis.ai.security.allow-default-tenant-in-corporate=true",
        "praxis.ai.security.server-default-tenant=palette-proof",
        "praxis.ai.security.server-default-environment=local",
        "praxis.ai.rag.vector-store.enabled=false",
        "praxis.ai.registry.bootstrap.enabled=false",
        "praxis.ai.registry.health.enabled=false",
        "spring.ai.embedding.provider=mock",
        "spring.ai.openai.api-key=dummy",
        "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:palette_proof_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "CONFIG_DATASOURCE_URL=jdbc:h2:mem:palette_proof_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:governed-color-palette-h2.sql'",
        "CONFIG_DATASOURCE_USERNAME=sa",
        "CONFIG_DATASOURCE_PASSWORD=",
        "config.datasource.url=jdbc:h2:mem:palette_proof_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:governed-color-palette-h2.sql'",
        "config.datasource.driver-class-name=org.h2.Driver",
        "config.datasource.username=sa",
        "config.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "config.jpa.hibernate.ddl-auto=none"
    })
class GovernedColorPaletteHttpIntegrationTest {

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenService jwtTokenService;

  @MockBean(name = "ragVectorStore") VectorStore ragVectorStore;
  @MockBean DomainRuleDefinitionApprovalRepository definitionApprovalRepository;

  @Test
  void authorsPreviewsPublishesAndRevalidatesPaletteOverRealHttp() {
    ResponseEntity<JsonNode> preview = exchange(
        "/api/praxis/config/color-palettes/previews",
        HttpMethod.POST,
        """
            {
              "paletteKey":"corporate-main",
              "palette":%s
            }
            """.formatted(palette()),
        "palette-author",
        "RULE_DEFINITION_AUTHOR",
        null);
    assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(preview.getBody().path("validation").path("valid").asBoolean()).isTrue();

    ResponseEntity<JsonNode> intake = exchange(
        "/api/praxis/config/domain-rules/intake",
        HttpMethod.POST,
        """
            {
              "prompt":"Author a governed corporate palette for semantic UI colors.",
              "assistantMessage":"The decision will be reviewed and materialized as a design-token catalog.",
              "ruleKey":"design.tokens.palette.corporate-main",
              "ruleType":"design_token_palette",
              "contextKey":"design-system",
              "resourceKey":"design.tokens",
              "serviceKey":"praxis-ui-angular",
              "parameters":{"paletteKey":"corporate-main","palette":%s},
              "governance":{"requiredApprovals":["design-council"]}
            }
            """.formatted(palette()),
        "palette-author",
        "RULE_DEFINITION_AUTHOR",
        null);
    assertThat(intake.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String definitionId = intake.getBody().path("definition").path("id").asText();
    assertThat(definitionId).isNotBlank();
    assertThat(intake.getBody().path("definition").path("parameters").path("paletteKey").asText())
        .describedAs(intake.getBody().toPrettyString())
        .isEqualTo("corporate-main");

    ResponseEntity<JsonNode> simulation = exchange(
        "/api/praxis/config/domain-rules/simulations",
        HttpMethod.POST,
        "{\"ruleDefinitionId\":\"" + definitionId + "\"}",
        "palette-author",
        "RULE_DEFINITION_AUTHOR",
        null);
    assertThat(simulation.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode predicted = simulation.getBody().path("predictedMaterializations").path(0);
    assertThat(predicted.path("targetLayer").asText()).isEqualTo("design_token_catalog");
    assertThat(predicted.path("targetArtifactKey").asText())
        .describedAs(simulation.getBody().toPrettyString())
        .isEqualTo("corporate-main");

    exchange(
        "/api/praxis/config/domain-rules/definitions/" + definitionId + "/status",
        HttpMethod.PATCH,
        "{\"status\":\"proposed\"}",
        "palette-author",
        "RULE_DEFINITION_AUTHOR",
        null);
    ResponseEntity<JsonNode> approval = exchange(
        "/api/praxis/config/domain-rules/definitions/" + definitionId + "/status",
        HttpMethod.PATCH,
        "{\"status\":\"approved\",\"validationResult\":{\"review\":\"approved\"}}",
        "design-council",
        "RULE_DEFINITION_APPROVER",
        null);
    assertThat(approval.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<JsonNode> publication = exchange(
        "/api/praxis/config/domain-rules/publications",
        HttpMethod.POST,
        "{\"ruleDefinitionId\":\"" + definitionId + "\",\"applyEligibleMaterializations\":true}",
        "release-manager",
        "RULE_SNAPSHOT_PUBLISHER",
        null);
    assertThat(publication.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publication.getBody().path("publicationStatus").asText())
        .describedAs(publication.getBody().toPrettyString()).isEqualTo("published");

    ResponseEntity<JsonNode> catalog = exchange(
        "/api/praxis/config/color-palettes/corporate-main",
        HttpMethod.GET,
        null,
        "palette-reader",
        "RULE_DEFINITION_READER",
        null);
    assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(catalog.getHeaders().getETag()).isNotBlank();
    assertThat(catalog.getBody().path("validation").path("valid").asBoolean()).isTrue();
    assertThat(catalog.getBody().path("entries")).hasSize(2);

    publishSibling("corporate-dark", "dark", "Dark", "#f4eff4", "#121014");
    publishSibling("corporate-high-contrast", "high-contrast", "High contrast", "#ffffff", "#000000");
    var family = exchange("/api/praxis/config/color-palettes?familyKey=corporate", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", null);
    assertThat(family.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(family.getBody()).hasSize(3);
    assertThat(family.getBody()).extracting(node -> node.path("variant").path("key").asText())
        .containsExactly("dark", "high-contrast", "light");

    ResponseEntity<JsonNode> notModified = exchange(
        "/api/praxis/config/color-palettes/corporate-main",
        HttpMethod.GET,
        null,
        "palette-reader",
        "RULE_DEFINITION_READER",
        catalog.getHeaders().getETag());
    assertThat(notModified.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);

    // A new version follows the same author/reviewer/publisher lifecycle while v1 stays readable.
    String secondPalette = palette().replace("#1b1b1f", "#000000");
    ResponseEntity<JsonNode> nextDefinition = exchange(
        "/api/praxis/config/domain-rules/definitions", HttpMethod.POST,
        """
            {
              "ruleKey":"design.tokens.palette.corporate-main",
              "version":2,"ruleType":"design_token_palette","status":"draft",
              "contextKey":"design-system","resourceKey":"design.tokens",
              "serviceKey":"praxis-ui-angular",
              "parameters":{"palette":%s},
              "governance":{"requiredApprovals":["design-council"]}
            }
            """.formatted(secondPalette),
        "palette-author", "RULE_DEFINITION_AUTHOR", null);
    assertThat(nextDefinition.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String secondId = nextDefinition.getBody().path("id").asText();
    String secondPublication = "{\"ruleDefinitionId\":\"" + secondId
        + "\",\"applyEligibleMaterializations\":true}";
    var blocked = exchange("/api/praxis/config/domain-rules/publications", HttpMethod.POST,
        secondPublication, "release-manager", "RULE_SNAPSHOT_PUBLISHER", null);
    assertThat(blocked.getBody().path("publicationStatus").asText()).isEqualTo("blocked");
    var proposed = exchange("/api/praxis/config/domain-rules/definitions/" + secondId + "/status",
        HttpMethod.PATCH, "{\"status\":\"proposed\"}", "palette-author", "RULE_DEFINITION_AUTHOR", null);
    assertThat(proposed.getStatusCode()).isEqualTo(HttpStatus.OK);
    var approved = exchange("/api/praxis/config/domain-rules/definitions/" + secondId + "/status",
        HttpMethod.PATCH, "{\"status\":\"approved\",\"validationResult\":{\"review\":\"approved\"}}",
        "design-council", "RULE_DEFINITION_APPROVER", null);
    assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
    var published = exchange("/api/praxis/config/domain-rules/publications", HttpMethod.POST,
        secondPublication, "release-manager", "RULE_SNAPSHOT_PUBLISHER", null);
    assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(published.getBody().path("publicationStatus").asText())
        .describedAs(published.getBody().toPrettyString()).isEqualTo("published");

    var latest = exchange("/api/praxis/config/color-palettes/corporate-main", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", catalog.getHeaders().getETag());
    assertThat(latest.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(latest.getBody().path("version").asInt()).isEqualTo(2);
    assertThat(latest.getHeaders().getETag()).isNotEqualTo(catalog.getHeaders().getETag());
    assertThat(latest.getBody().path("entries").path(0).path("fallback").asText()).isEqualTo("#000000");
    var old = exchange("/api/praxis/config/color-palettes/corporate-main?version=1", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", null);
    assertThat(old.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(old.getBody()).isEqualTo(catalog.getBody());
    assertThat(old.getHeaders().getETag()).isEqualTo(catalog.getHeaders().getETag());
    var oldNotModified = exchange("/api/praxis/config/color-palettes/corporate-main?version=1", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", catalog.getHeaders().getETag());
    assertThat(oldNotModified.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    var currentNotModified = exchange("/api/praxis/config/color-palettes/corporate-main?version=2", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", latest.getHeaders().getETag());
    assertThat(currentNotModified.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    var all = exchange("/api/praxis/config/color-palettes", HttpMethod.GET,
        null, "palette-reader", "RULE_DEFINITION_READER", null);
    assertThat(all.getBody()).hasSize(3);
    assertThat(all.getBody()).anySatisfy(node -> {
      assertThat(node.path("paletteKey").asText()).isEqualTo("corporate-main");
      assertThat(node.path("version").asInt()).isEqualTo(2);
    });
    var republishOld = exchange("/api/praxis/config/domain-rules/publications", HttpMethod.POST,
        "{\"ruleDefinitionId\":\"" + definitionId + "\",\"applyEligibleMaterializations\":true}",
        "release-manager", "RULE_SNAPSHOT_PUBLISHER", null);
    assertThat(republishOld.getBody().path("publicationStatus").asText()).isEqualTo("blocked");
  }

  private ResponseEntity<JsonNode> exchange(
      String path,
      HttpMethod method,
      String body,
      String subject,
      String authority,
      String ifNoneMatch) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
        subject, "USER", List.of("ROLE_" + authority)));
    if (ifNoneMatch != null) headers.set(HttpHeaders.IF_NONE_MATCH, ifNoneMatch);
    return restTemplate.exchange(
        path,
        method,
        new HttpEntity<>(body, headers),
        JsonNode.class);
  }

  private String palette() {
    return """
        {
          "paletteKey":"corporate-main",
          "displayName":"Corporate Main",
          "familyKey":"corporate",
          "variant":{"key":"light","displayName":"Light","dimensions":{"colorScheme":"light","contrast":"standard"}},
          "scope":{"brand":"praxis","theme":"adaptive"},
          "entries":[
            {"tokenId":"content.primary","displayName":"Primary content","aliases":["Body text"],"semanticRole":"content","cssReference":"var(--content-primary)","fallback":"#1b1b1f","purposes":["text"]},
            {"tokenId":"surface.default","displayName":"Default surface","aliases":[],"semanticRole":"surface","cssReference":"var(--surface-default)","fallback":"#ffffff","purposes":["surface"]}
          ],
          "contrastEvidence":[
            {"foregroundTokenId":"content.primary","backgroundTokenId":"surface.default","ratio":16.4,"requiredLevel":"AAA"}
          ],
          "provenance":{"source":"Praxis Design System","approvedBy":"Design Council"}
        }
        """;
  }

  private void publishSibling(
      String paletteKey,
      String variantKey,
      String variantDisplayName,
      String foreground,
      String background) {
    String sibling = palette()
        .replace("corporate-main", paletteKey)
        .replace("Corporate Main", "Corporate " + variantDisplayName)
        .replace("\"key\":\"light\"", "\"key\":\"" + variantKey + "\"")
        .replace("\"displayName\":\"Light\"", "\"displayName\":\"" + variantDisplayName + "\"")
        .replace("#1b1b1f", "__FOREGROUND__")
        .replace("#ffffff", background)
        .replace("__FOREGROUND__", foreground);
    var definition = exchange("/api/praxis/config/domain-rules/definitions", HttpMethod.POST,
        """
            {"ruleKey":"design.tokens.palette.%s","version":1,"ruleType":"design_token_palette",
             "status":"draft","contextKey":"design-system","resourceKey":"design.tokens",
             "serviceKey":"praxis-ui-angular","parameters":{"palette":%s},
             "governance":{"requiredApprovals":["design-council"]}}
            """.formatted(paletteKey, sibling),
        "palette-author", "RULE_DEFINITION_AUTHOR", null);
    assertThat(definition.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String id = definition.getBody().path("id").asText();
    assertThat(exchange("/api/praxis/config/domain-rules/definitions/" + id + "/status", HttpMethod.PATCH,
        "{\"status\":\"proposed\"}", "palette-author", "RULE_DEFINITION_AUTHOR", null).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(exchange("/api/praxis/config/domain-rules/definitions/" + id + "/status", HttpMethod.PATCH,
        "{\"status\":\"approved\",\"validationResult\":{\"review\":\"approved\"}}",
        "design-council", "RULE_DEFINITION_APPROVER", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    var publication = exchange("/api/praxis/config/domain-rules/publications", HttpMethod.POST,
        "{\"ruleDefinitionId\":\"" + id + "\",\"applyEligibleMaterializations\":true}",
        "release-manager", "RULE_SNAPSHOT_PUBLISHER", null);
    assertThat(publication.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publication.getBody().path("publicationStatus").asText())
        .describedAs(publication.getBody().toPrettyString()).isEqualTo("published");
  }
}
