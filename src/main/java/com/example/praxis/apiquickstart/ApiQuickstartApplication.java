package com.example.praxis.apiquickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Ponto de entrada do host operacional de referencia da plataforma Praxis.
 *
 * <p>Este aplicativo sobe, em um unico processo Spring Boot, o dominio de exemplo do quickstart
 * junto com os dois starters principais usados pela plataforma:</p>
 *
 * <ul>
 *   <li>{@code praxis-metadata-starter}, responsavel por OpenAPI enriquecido, {@code x-ui},
 *   {@code /schemas/filtered}, discovery e capabilities;</li>
 *   <li>{@code praxis-config-starter}, responsavel por {@code /api/praxis/config/**},
 *   persistencia de configuracao, catalogos e fluxos de AI/context.</li>
 * </ul>
 *
 * <p>Por isso, o quickstart nao e so um "hello world". Ele funciona como host publico e didatico
 * para demonstrar como um backend real da plataforma deve compor metadata, config, seguranca,
 * paths canonicos e recursos concretos.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.praxis.apiquickstart",
        "org.praxisplatform.config",
        "org.praxisplatform.uischema"
})
public class ApiQuickstartApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiQuickstartApplication.class, args);
    }
}
