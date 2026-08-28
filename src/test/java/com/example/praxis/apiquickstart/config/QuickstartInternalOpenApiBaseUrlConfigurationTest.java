package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Prova que consumidores server-side do OpenAPI recebem uma origem interna no perfil de
 * produção mesmo quando o deployment não declara um override. Isso inclui o prewarm assíncrono,
 * que não possui contexto HTTP e portanto não pode depender de {@code RequestContextHolder}.
 */
class QuickstartInternalOpenApiBaseUrlConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(QuickstartInternalOpenApiBaseUrlConfigurationTest::addApplicationProperties)
            .withPropertyValues("PORT=10000");

    @Test
    void shouldResolveEveryServerSideOpenApiConsumerAgainstTheInternalListener() {
        contextRunner
                .withInitializer(QuickstartInternalOpenApiBaseUrlConfigurationTest::addProductionProperties)
                .run(context -> {
            assertEquals(
                    "http://127.0.0.1:10000",
                    context.getEnvironment().getProperty("app.openapi.internal-base-url"));
            assertEquals(
                    "http://127.0.0.1:10000",
                    context.getEnvironment().getProperty("praxis.ai.schemas.base-url"));
            assertEquals(
                    "http://127.0.0.1:10000",
                    context.getEnvironment().getProperty("praxis.ai.capabilities.base-url"));
            assertEquals(
                    "false",
                    context.getEnvironment().getProperty("praxis.openapi.prewarm.enabled"));
                });
    }

    @Test
    void shouldExposeDeploymentOriginsAsAdditionsForBothHostAllowLists() {
        contextRunner
                .withPropertyValues(
                        "CORS_ALLOWED_ORIGINS=https://deployment.example",
                        "APP_SECURITY_CONFIG_ORIGIN_RESTRICTION_ALLOWED_ORIGINS=https://deployment.example")
                .run(context -> {
                    String corsOrigins = context.getEnvironment().getProperty("app.cors.allowed-origins", "");
                    String configOrigins = context.getEnvironment().getProperty(
                            "app.security.config-origin-restriction.allowed-origins",
                            "");

                    assertEquals("https://deployment.example", corsOrigins);
                    assertEquals("https://deployment.example", configOrigins);
                    assertFalse(corsOrigins.contains("*"));
                });
    }

    private static void addApplicationProperties(
            org.springframework.context.ConfigurableApplicationContext context) {
        addPropertySource(context, "application.properties", false);
    }

    private static void addProductionProperties(
            org.springframework.context.ConfigurableApplicationContext context) {
        addPropertySource(context, "application-prod.properties", true);
    }

    private static void addPropertySource(
            org.springframework.context.ConfigurableApplicationContext context,
            String resourceName,
            boolean highestPrecedence) {
        try {
            ResourcePropertySource propertySource =
                    new ResourcePropertySource(new ClassPathResource(resourceName));
            if (highestPrecedence) {
                context.getEnvironment().getPropertySources().addFirst(propertySource);
            } else {
                context.getEnvironment().getPropertySources().addLast(propertySource);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Quickstart properties: " + resourceName, exception);
        }
    }
}
