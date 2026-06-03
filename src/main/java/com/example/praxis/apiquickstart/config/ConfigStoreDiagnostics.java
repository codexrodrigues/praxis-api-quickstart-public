package com.example.praxis.apiquickstart.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Runner de diagnostico local para o config-store hospedado pelo quickstart.
 *
 * <p>Ativo apenas em {@code dev}, este componente existe para tornar legivel a integracao entre o
 * host principal e o {@code praxis-config-starter}: data sources, Flyway, tabelas esperadas e
 * detalhes de conexao. Ele ajuda a reduzir ambiguidade quando o ambiente local esta misturando
 * configuracao da API principal e da superficie {@code /api/praxis/config/**}.</p>
 */
@Component
@Profile("dev")
@Slf4j
public class ConfigStoreDiagnostics implements ApplicationRunner, EnvironmentAware {

    private final DataSource configDataSource;
    private final DataSourceProperties configDataSourceProperties;
    private final DataSource apiDataSource;
    private final DataSourceProperties apiDataSourceProperties;
    private final ApplicationContext applicationContext;

    private Environment environment;

    @Autowired
    public ConfigStoreDiagnostics(
            @Qualifier("configDataSource") DataSource configDataSource,
            @Qualifier("configDataSourceProperties") DataSourceProperties configDataSourceProperties,
            @Qualifier("apiDataSource") DataSource apiDataSource,
            @Qualifier("apiDataSourceProperties") DataSourceProperties apiDataSourceProperties,
            ApplicationContext applicationContext) {
        this.configDataSource = configDataSource;
        this.configDataSourceProperties = configDataSourceProperties;
        this.apiDataSource = apiDataSource;
        this.apiDataSourceProperties = apiDataSourceProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        logConfigProperties();
        logFlywayStatus();
        logDatabaseStatus();
    }

    /** Registra propriedades relevantes para entender a composicao do config-store local. */
    private void logConfigProperties() {
        String configUrl = environment.getProperty("config.datasource.url");
        String flywayUrl = environment.getProperty("spring.flyway.url");
        String flywayLocations = environment.getProperty("spring.flyway.locations");
        String flywayEnabled = environment.getProperty("spring.flyway.enabled", "true");

        log.info("[CONFIG-STORE] config.datasource.url={}", sanitizeJdbcUrl(configUrl));
        log.info(
                "[CONFIG-STORE] configDataSourceProperties.url={}",
                sanitizeJdbcUrl(configDataSourceProperties.getUrl()));
        log.info(
                "[CONFIG-STORE] apiDataSourceProperties.url={}",
                sanitizeJdbcUrl(apiDataSourceProperties.getUrl()));
        log.info("[CONFIG-STORE] spring.flyway.url={}", sanitizeJdbcUrl(flywayUrl));
        log.info("[CONFIG-STORE] spring.flyway.locations={}", flywayLocations);
        log.info("[CONFIG-STORE] spring.flyway.enabled={}", flywayEnabled);
    }

    /** Mostra se Flyway e os beans esperados do host realmente foram carregados. */
    private void logFlywayStatus() {
        boolean flywayClassPresent =
                ClassUtils.isPresent("org.flywaydb.core.Flyway", getClass().getClassLoader());
        log.info("[CONFIG-STORE] Flyway class present={}", flywayClassPresent);
        boolean hasFlywayBean = applicationContext.containsBean("flyway");
        log.info("[CONFIG-STORE] Flyway bean present={}", hasFlywayBean);
        log.info(
                "[CONFIG-STORE] DataSource beans={}",
                String.join(", ", applicationContext.getBeanNamesForType(DataSource.class)));
    }

    /** Abre conexoes reais e confirma o estado minimo esperado do banco de configuracao. */
    private void logDatabaseStatus() {
        log.info(
                "[CONFIG-STORE] configDataSource==apiDataSource {}",
                configDataSource == apiDataSource);
        logDataSourceDetails("CONFIG-STORE", configDataSource);
        logDataSourceDetails("API-DB", apiDataSource);

        try (Connection connection = configDataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            log.info("[CONFIG-STORE] connected url={}", meta.getURL());
            log.info("[CONFIG-STORE] connected user={}", meta.getUserName());
        } catch (Exception e) {
            log.error("[CONFIG-STORE] failed to open connection for diagnostics", e);
            return;
        }

        try (Connection connection = apiDataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            log.info("[API-DB] connected url={}", meta.getURL());
            log.info("[API-DB] connected user={}", meta.getUserName());
        } catch (Exception e) {
            log.error("[API-DB] failed to open connection for diagnostics", e);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(configDataSource);
        try {
            String dbName = jdbcTemplate.queryForObject("select current_database()", String.class);
            log.info("[CONFIG-STORE] current_database={}", dbName);
        } catch (Exception e) {
            log.warn("[CONFIG-STORE] unable to query current_database()", e);
        }

        logTablePresence(jdbcTemplate, "ai_registry");
        logTablePresence(jdbcTemplate, "ui_user_config");
        logTablePresence(jdbcTemplate, "flyway_schema_history");
        logFlywayHistoryCount(jdbcTemplate);
    }

    private void logDataSourceDetails(String label, DataSource dataSource) {
        log.info(
                "[{}] dataSource.class={} identity={}",
                label,
                dataSource.getClass().getName(),
                System.identityHashCode(dataSource));
        if (dataSource instanceof HikariDataSource hikari) {
            log.info("[{}] hikari.jdbcUrl={}", label, sanitizeJdbcUrl(hikari.getJdbcUrl()));
            log.info("[{}] hikari.poolName={}", label, hikari.getPoolName());
        }
    }

    private void logTablePresence(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            Integer count =
                    jdbcTemplate.queryForObject(
                            """
                            select count(*)
                            from information_schema.tables
                            where table_schema = 'public' and table_name = ?
                            """,
                            Integer.class,
                            tableName);
            log.info(
                    "[CONFIG-STORE] table {} present={}",
                    tableName,
                    count != null && count > 0);
        } catch (Exception e) {
            log.warn("[CONFIG-STORE] unable to check table {}", tableName, e);
        }
    }

    private void logFlywayHistoryCount(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from flyway_schema_history",
                    Integer.class);
            log.info("[CONFIG-STORE] flyway_schema_history rows={}", count);
        } catch (Exception e) {
            log.warn("[CONFIG-STORE] unable to query flyway_schema_history", e);
        }
    }

    /** Evita expor credenciais completas quando a URL JDBC e logada. */
    private String sanitizeJdbcUrl(String raw) {
        if (raw == null) return null;
        int atIndex = raw.indexOf('@');
        if (atIndex > 0 && raw.startsWith("jdbc:")) {
            return "jdbc:***" + raw.substring(atIndex);
        }
        return raw;
    }
}
