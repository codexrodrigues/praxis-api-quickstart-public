package com.example.praxis.apiquickstart.config;

import org.praxisplatform.uischema.determination.ReactiveDeterminationDefinition;
import org.praxisplatform.uischema.determination.ReactiveDeterminationDefinitionProvider;
import org.praxisplatform.uischema.determination.ReactiveDeterminationFormMode;
import org.praxisplatform.uischema.determination.ReactiveDeterminationInputBinding;
import org.praxisplatform.uischema.determination.ReactiveDeterminationOutputBinding;
import org.praxisplatform.uischema.determination.ReactiveDeterminationProvenance;
import org.praxisplatform.uischema.determination.ReactiveDeterminationProvenanceKind;
import org.praxisplatform.uischema.determination.ReactiveDeterminationScope;
import org.praxisplatform.uischema.determination.ReactiveDeterminationTriggerMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Pilotos host-neutral que vinculam formularios de dominios distintos a capabilities backend.
 *
 * <p>O provider declara somente identidades canonicas e JSON Pointers. URL, metodo e schemas
 * executaveis sao resolvidos pelo Metadata Starter a partir do OpenAPI do proprio host.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ReactiveDeterminationPilotConfiguration {

    @Bean
    ReactiveDeterminationDefinitionProvider postalAddressReactiveDeterminationProvider() {
        return () -> List.of(new ReactiveDeterminationDefinition(
                "human-resources.address.by-postal-code",
                List.of(
                        new ReactiveDeterminationScope(
                                "createAddress",
                                ReactiveDeterminationFormMode.CREATE),
                        new ReactiveDeterminationScope(
                                "updateAddress",
                                ReactiveDeterminationFormMode.EDIT)
                ),
                "determinePostalAddress",
                ReactiveDeterminationTriggerMode.ON_CHANGE,
                List.of("/cep"),
                List.of(new ReactiveDeterminationInputBinding("/cep", "/cep")),
                List.of(
                        new ReactiveDeterminationOutputBinding("/logradouro", "/logradouro"),
                        new ReactiveDeterminationOutputBinding("/bairro", "/bairro"),
                        new ReactiveDeterminationOutputBinding("/cidade", "/cidade"),
                        new ReactiveDeterminationOutputBinding("/estado", "/estado")
                ),
                new ReactiveDeterminationProvenance(
                        ReactiveDeterminationProvenanceKind.HOST,
                        "praxis-api-quickstart",
                        "1")
        ));
    }

    @Bean
    ReactiveDeterminationDefinitionProvider payrollNetSalaryReactiveDeterminationProvider() {
        return () -> List.of(new ReactiveDeterminationDefinition(
                "human-resources.payroll.net-salary",
                List.of(
                        new ReactiveDeterminationScope(
                                "createPayroll",
                                ReactiveDeterminationFormMode.CREATE),
                        new ReactiveDeterminationScope(
                                "updatePayroll",
                                ReactiveDeterminationFormMode.EDIT)
                ),
                "determinePayrollNetSalary",
                ReactiveDeterminationTriggerMode.ON_CHANGE,
                List.of("/salarioBruto", "/totalDescontos"),
                List.of(
                        new ReactiveDeterminationInputBinding("/salarioBruto", "/salarioBruto"),
                        new ReactiveDeterminationInputBinding("/totalDescontos", "/totalDescontos")
                ),
                List.of(new ReactiveDeterminationOutputBinding(
                        "/salarioLiquido",
                        "/salarioLiquido")),
                new ReactiveDeterminationProvenance(
                        ReactiveDeterminationProvenanceKind.HOST,
                        "praxis-api-quickstart",
                        "1")
        ));
    }

    @Bean
    ReactiveDeterminationDefinitionProvider payrollPaymentDateReactiveDeterminationProvider() {
        return () -> List.of(new ReactiveDeterminationDefinition(
                "human-resources.payroll.payment-date",
                List.of(
                        new ReactiveDeterminationScope(
                                "createPayroll",
                                ReactiveDeterminationFormMode.CREATE),
                        new ReactiveDeterminationScope(
                                "updatePayroll",
                                ReactiveDeterminationFormMode.EDIT)
                ),
                "determinePayrollPaymentDate",
                ReactiveDeterminationTriggerMode.ON_CHANGE,
                List.of("/ano", "/mes", "/salarioLiquido"),
                List.of(
                        new ReactiveDeterminationInputBinding("/ano", "/ano"),
                        new ReactiveDeterminationInputBinding("/mes", "/mes"),
                        new ReactiveDeterminationInputBinding("/salarioLiquido", "/salarioLiquido")
                ),
                List.of(new ReactiveDeterminationOutputBinding(
                        "/dataPagamento",
                        "/dataPagamento")),
                new ReactiveDeterminationProvenance(
                        ReactiveDeterminationProvenanceKind.HOST,
                        "praxis-api-quickstart",
                        "1")
        ));
    }
}
