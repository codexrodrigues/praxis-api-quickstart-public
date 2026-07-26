package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class DataSourceConfigTest {

    @Test
    void configPersistenceUnitInheritsGovernedJpaBatchingDefaults() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("config.datasource.url", ""))
                .thenReturn("jdbc:postgresql://localhost/praxis");
        when(environment.getProperty("config.jpa.hibernate.ddl-auto", ""))
                .thenReturn("");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                .thenReturn("none");
        when(environment.getProperty("config.jpa.properties.hibernate.jdbc.batch_size"))
                .thenReturn(null);
        when(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
                .thenReturn("100");
        when(environment.getProperty("config.jpa.properties.hibernate.order_inserts"))
                .thenReturn(null);
        when(environment.getProperty("spring.jpa.properties.hibernate.order_inserts"))
                .thenReturn("true");
        when(environment.getProperty("config.jpa.properties.hibernate.order_updates"))
                .thenReturn(null);
        when(environment.getProperty("spring.jpa.properties.hibernate.order_updates"))
                .thenReturn("true");

        Map<String, Object> properties = new DataSourceConfig().jpaVendorProperties(
                environment,
                "config.datasource.url",
                "config.jpa.hibernate.ddl-auto",
                "config.jpa.properties");

        assertThat(properties)
                .containsEntry("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .containsEntry("hibernate.hbm2ddl.auto", "none")
                .containsEntry("hibernate.jdbc.batch_size", "100")
                .containsEntry("hibernate.order_inserts", "true")
                .containsEntry("hibernate.order_updates", "true");
    }

    @Test
    void configPersistenceUnitCanOverrideGlobalBatchSize() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("config.datasource.url", ""))
                .thenReturn("jdbc:postgresql://localhost/praxis");
        when(environment.getProperty("config.jpa.hibernate.ddl-auto", ""))
                .thenReturn("");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                .thenReturn("");
        when(environment.getProperty("config.jpa.properties.hibernate.jdbc.batch_size"))
                .thenReturn("25");

        Map<String, Object> properties = new DataSourceConfig().jpaVendorProperties(
                environment,
                "config.datasource.url",
                "config.jpa.hibernate.ddl-auto",
                "config.jpa.properties");

        assertThat(properties).containsEntry("hibernate.jdbc.batch_size", "25");
    }
}
