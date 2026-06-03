package com.example.praxis.apiquickstart.config;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.springframework.jdbc.core.JdbcTemplate; // Add import

@Configuration
@EnableTransactionManagement
/**
 * Configura os dois data sources usados pelo quickstart.
 *
 * <p>O quickstart hospeda, no mesmo processo, dois conjuntos de persistencia com papeis
 * diferentes:</p>
 *
 * <ul>
 *   <li>o data source {@code api}, que guarda as entidades do dominio de exemplo expostas pelo
 *   proprio quickstart;</li>
 *   <li>o data source {@code config}, que hospeda as tabelas e repositories do
 *   {@code praxis-config-starter}.</li>
 * </ul>
 *
 * <p>Essa separacao e pedagogica e operacional. Ela deixa claro que o host integra as duas
 * superficies, mas o dominio do quickstart e a persistencia canonica de
 * {@code /api/praxis/config/**} continuam sendo responsabilidades distintas.</p>
 */
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties apiDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "apiDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource apiDataSource(@Qualifier("apiDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "apiEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean apiEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("apiDataSource") DataSource dataSource,
            Environment environment) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.praxis.apiquickstart")
                .persistenceUnit("api")
                .properties(jpaVendorProperties(environment, "spring.datasource.url", "spring.jpa.hibernate.ddl-auto"))
                .build();
    }

    @Primary
    @Bean(name = "apiTransactionManager")
    public PlatformTransactionManager apiTransactionManager(
            @Qualifier("apiEntityManagerFactory") LocalContainerEntityManagerFactoryBean apiEntityManagerFactory) {
        return new JpaTransactionManager(apiEntityManagerFactory.getObject());
    }

    @Bean(name = "configJdbcTemplate")
    public JdbcTemplate configJdbcTemplate(@Qualifier("configDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "apiJdbcTemplate")
    public JdbcTemplate apiJdbcTemplate(@Qualifier("apiDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConfigurationProperties(prefix = "config.datasource")
    public DataSourceProperties configDataSourceProperties() {
        return new DataSourceProperties();
    }
// ... rest of file

    @Bean(name = "configDataSource")
    @ConfigurationProperties(prefix = "config.datasource.hikari")
    public DataSource configDataSource(
            @Qualifier("configDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "configEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean configEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("configDataSource") DataSource dataSource,
            Environment environment) {
        return builder
                .dataSource(dataSource)
                .packages("org.praxisplatform.config.domain")
                .persistenceUnit("config")
                .properties(jpaVendorProperties(environment, "config.datasource.url", "config.jpa.hibernate.ddl-auto"))
                .build();
    }

    private Map<String, Object> jpaVendorProperties(
            Environment environment,
            String datasourceUrlProperty,
            String ddlAutoProperty) {
        Map<String, Object> properties = new HashMap<>();
        String datasourceUrl = environment.getProperty(datasourceUrlProperty, "");
        boolean h2 = datasourceUrl.startsWith("jdbc:h2:");
        properties.put("hibernate.dialect", h2
                ? "org.hibernate.dialect.H2Dialect"
                : "org.hibernate.dialect.PostgreSQLDialect");
        String ddlAuto = environment.getProperty(
                ddlAutoProperty,
                environment.getProperty("spring.jpa.hibernate.ddl-auto", ""));
        if (!ddlAuto.isBlank()) {
            properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        }
        return properties;
    }

    @Bean(name = "configTransactionManager")
    public PlatformTransactionManager configTransactionManager(
            @Qualifier("configEntityManagerFactory") LocalContainerEntityManagerFactoryBean configEntityManagerFactory) {
        return new JpaTransactionManager(configEntityManagerFactory.getObject());
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.example.praxis.apiquickstart",
            entityManagerFactoryRef = "apiEntityManagerFactory",
            transactionManagerRef = "apiTransactionManager")
    @EntityScan(basePackages = "com.example.praxis.apiquickstart")
    static class ApiJpaConfig {
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "org.praxisplatform.config.repository",
            entityManagerFactoryRef = "configEntityManagerFactory",
            transactionManagerRef = "configTransactionManager")
    @EntityScan(basePackages = "org.praxisplatform.config.domain")
    /** Liga repositories e entidades publicados pelo {@code praxis-config-starter}. */
    static class ConfigJpaConfig {
    }
}
