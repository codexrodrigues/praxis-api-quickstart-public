package com.example.praxis.apiquickstart.hr.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Provisions the disposable PostgreSQL database used by the full absence-comparison E2E gate.
 *
 * <p>The semantic golden suite remains the single fixture source. This utility only materializes
 * its explicit resource identities into the real Quickstart schema and migrations.</p>
 */
public final class AbsenceAnalyticsE2eDatabaseFixture {

    private static final Pattern SAFE_DATABASE_NAME =
            Pattern.compile("praxis_e2e_absence_[a-z0-9_]{1,42}");
    private static final Pattern RUNTIME_ROLE_GRANT = Pattern.compile(
            "(?i)^\\s*grant\\s+(?:execute\\s+on\\s+function\\s+public\\.hr_absence_criticality_level\\(bigint\\)"
                    + "|select\\s+on\\s+public\\.vw_analytics_afastamentos)\\s+to\\s+praxis_service_user;\\s*$");
    private static final Path GOLDEN_PATH = Path.of(
            "src/test/resources/absence-analytics-lab/absence-analytics-semantic-golden-suite.json");
    private static final Path ASSIGNMENT_MIGRATION = Path.of(
            "db/operational-migrations/V20260714_001__historical_department_assignments.sql");
    private static final Path ANALYTICS_MIGRATION = Path.of(
            "db/operational-migrations/V20260715_005__absence_analytics_unique_days_policy.sql");

    private AbsenceAnalyticsE2eDatabaseFixture() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !("provision".equals(args[0]) || "drop".equals(args[0]))) {
            throw new IllegalArgumentException(
                    "Usage: AbsenceAnalyticsE2eDatabaseFixture <provision|drop> <database> [scenario]");
        }
        String command = args[0];
        String databaseName = validateDatabaseName(args[1]);
        DatabaseCredentials credentials = DatabaseCredentials.fromEnvironment(System.getenv());

        if ("drop".equals(command)) {
            dropDatabase(credentials, databaseName);
            System.out.printf("DROPPED database=%s%n", databaseName);
            return;
        }

        String scenarioId = args.length >= 3 ? args[2] : "ALG-01";
        FixtureScenario scenario = loadScenario(GOLDEN_PATH, scenarioId);
        provision(credentials, databaseName, scenario);
        System.out.printf(
                "PROVISIONED database=%s scenario=%s employees=%d departments=%d absences=%d%n",
                databaseName,
                scenario.id(),
                scenario.employees().size(),
                scenario.departments().size(),
                scenario.absences().size());
    }

    static FixtureScenario loadScenario(Path suitePath, String scenarioId) throws IOException {
        JsonNode suite = new ObjectMapper().readTree(Files.readString(suitePath));
        JsonNode scenario = null;
        for (JsonNode candidate : suite.path("scenarios")) {
            if (scenarioId.equals(candidate.path("id").asText())) {
                scenario = candidate;
                break;
            }
        }
        if (scenario == null) {
            throw new IllegalArgumentException("Golden scenario not found: " + scenarioId);
        }

        Map<String, EmployeeFixture> employees = new LinkedHashMap<>();
        for (JsonNode employee : scenario.path("employees")) {
            EmployeeFixture fixture = new EmployeeFixture(
                    employee.path("id").asText(),
                    employee.path("resourceId").asInt(),
                    employee.path("name").asText());
            requirePositiveIdentity(fixture.semanticId(), fixture.resourceId());
            if (employees.putIfAbsent(fixture.semanticId(), fixture) != null) {
                throw new IllegalArgumentException("Duplicate employee identity: " + fixture.semanticId());
            }
        }

        Map<String, DepartmentFixture> departments = new LinkedHashMap<>();
        for (JsonNode department : scenario.path("expected").path("departments")) {
            DepartmentFixture fixture = new DepartmentFixture(
                    department.path("departmentId").asText(),
                    department.path("resourceId").asInt(),
                    department.path("departmentName").asText());
            requirePositiveIdentity(fixture.semanticId(), fixture.resourceId());
            DepartmentFixture known = departments.putIfAbsent(fixture.semanticId(), fixture);
            if (known != null && !known.equals(fixture)) {
                throw new IllegalArgumentException("Conflicting department identity: " + fixture.semanticId());
            }
        }

        List<AssignmentFixture> assignments = new ArrayList<>();
        for (JsonNode assignment : scenario.path("assignments")) {
            String employeeId = assignment.path("employeeId").asText();
            String departmentId = assignment.path("departmentId").asText();
            if (!employees.containsKey(employeeId)) {
                throw new IllegalArgumentException("Assignment references unknown employee: " + employeeId);
            }
            DepartmentFixture department = departments.get(departmentId);
            if (department == null) {
                throw new IllegalArgumentException(
                        "Assignment has no expected resource identity for department: " + departmentId);
            }
            if (!department.name().equals(assignment.path("departmentName").asText())) {
                throw new IllegalArgumentException("Conflicting department name: " + departmentId);
            }
            assignments.add(new AssignmentFixture(
                    employeeId,
                    departmentId,
                    LocalDate.parse(assignment.path("validFrom").asText()),
                    assignment.hasNonNull("validTo")
                            ? LocalDate.parse(assignment.path("validTo").asText())
                            : null));
        }

        List<AbsenceFixture> absences = new ArrayList<>();
        for (JsonNode absence : scenario.path("absences")) {
            String employeeId = absence.path("employeeId").asText();
            if (!employees.containsKey(employeeId)) {
                throw new IllegalArgumentException("Absence references unknown employee: " + employeeId);
            }
            JsonNode restricted = absence.path("restricted");
            absences.add(new AbsenceFixture(
                    absence.path("id").asText(),
                    employeeId,
                    LocalDate.parse(absence.path("start").asText()),
                    LocalDate.parse(absence.path("end").asText()),
                    restricted.path("tipo").asText(),
                    restricted.path("observacoes").asText()));
        }

        ensureUniqueResourceIds(employees, departments);
        return new FixtureScenario(
                scenarioId,
                LocalDate.parse(scenario.path("nowDate").asText()),
                Map.copyOf(employees),
                Map.copyOf(departments),
                List.copyOf(assignments),
                List.copyOf(absences));
    }

    static String withoutRuntimeRoleGrants(String migration) {
        return migration.lines()
                .filter(line -> !RUNTIME_ROLE_GRANT.matcher(line).matches())
                .reduce("", (result, line) -> result + line + System.lineSeparator());
    }

    static String replaceDatabaseName(String jdbcUrl, String databaseName) {
        validateDatabaseName(databaseName);
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("CONFIG_DATASOURCE_URL must be a PostgreSQL JDBC URL.");
        }
        try {
            URI source = new URI(jdbcUrl.substring("jdbc:".length()));
            URI target = new URI(
                    source.getScheme(),
                    source.getUserInfo(),
                    source.getHost(),
                    source.getPort(),
                    "/" + databaseName,
                    source.getQuery(),
                    source.getFragment());
            return "jdbc:" + target;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("CONFIG_DATASOURCE_URL is not a valid JDBC URL.", exception);
        }
    }

    private static void provision(
            DatabaseCredentials credentials,
            String databaseName,
            FixtureScenario scenario
    ) throws Exception {
        boolean databaseCreated = false;
        try {
            createDatabase(credentials, databaseName);
            databaseCreated = true;
            String targetUrl = replaceDatabaseName(credentials.url(), databaseName);
            try (Connection connection = DriverManager.getConnection(
                    targetUrl, credentials.username(), credentials.password())) {
                connection.setAutoCommit(false);
                try {
                    execute(connection, "create extension if not exists vector");
                    execute(connection, "create extension if not exists btree_gist");
                    execute(connection, baseOperationalSchema());
                    seedBaseResources(connection, scenario);
                    execute(connection, Files.readString(ASSIGNMENT_MIGRATION));
                    seedAssignments(connection, scenario);
                    seedAbsences(connection, scenario);

                    // The production migration owns its grants. This disposable database lives on
                    // a server without the production runtime role; its dedicated grant test remains
                    // AbsenceAnalyticsPostgresOperationalProofTest.
                    execute(connection, withoutRuntimeRoleGrants(Files.readString(ANALYTICS_MIGRATION)));
                    execute(connection, heroProfileView());
                    verifyProjection(connection, scenario);
                    connection.commit();
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        } catch (Exception exception) {
            if (databaseCreated) {
                try {
                    dropDatabase(credentials, databaseName);
                } catch (Exception cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw exception;
        }
    }

    private static void createDatabase(DatabaseCredentials credentials, String databaseName)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                credentials.url(), credentials.username(), credentials.password());
             PreparedStatement existing = connection.prepareStatement(
                     "select 1 from pg_database where datname = ?")) {
            existing.setString(1, databaseName);
            try (ResultSet result = existing.executeQuery()) {
                if (result.next()) {
                    throw new IllegalStateException("Disposable database already exists: " + databaseName);
                }
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("create database " + quoteIdentifier(databaseName)
                        + " template template0 encoding 'UTF8'");
            }
        }
    }

    private static void dropDatabase(DatabaseCredentials credentials, String databaseName)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                credentials.url(), credentials.username(), credentials.password())) {
            try (PreparedStatement terminate = connection.prepareStatement("""
                    select pg_terminate_backend(pid)
                    from pg_stat_activity
                    where datname = ? and pid <> pg_backend_pid()
                    """)) {
                terminate.setString(1, databaseName);
                terminate.execute();
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("drop database if exists " + quoteIdentifier(databaseName));
            }
        }
    }

    private static void seedBaseResources(Connection connection, FixtureScenario scenario)
            throws SQLException {
        execute(connection, """
                insert into public.cargos (id, nome, nivel, descricao, salario_minimo, salario_maximo)
                values (1, 'Especialista Praxis', 'SENIOR', 'Cargo sintetico do gate E2E', 10000, 20000)
                """);

        try (PreparedStatement statement = connection.prepareStatement("""
                insert into public.departamentos (id, nome, codigo, responsavel_id)
                values (?, ?, ?, null)
                """)) {
            for (DepartmentFixture department : scenario.departments().values()) {
                statement.setInt(1, department.resourceId());
                statement.setString(2, department.name());
                statement.setString(3, department.semanticId());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                insert into public.funcionarios (
                    id, version, nome_completo, cpf, data_nascimento, email, telefone, salario,
                    data_admissao, ativo, cargo_id, departamento_id, foto_perfil_url,
                    estado_civil, pais_nascimento, cidade_nascimento
                ) values (?, 0, ?, ?, date '1990-01-01', ?, ?, 12000,
                          date '2020-01-01', true, 1, ?, null, null, 'Brasil', 'Sao Paulo')
                """)) {
            for (EmployeeFixture employee : scenario.employees().values()) {
                DepartmentFixture currentDepartment = currentDepartment(scenario, employee.semanticId());
                statement.setInt(1, employee.resourceId());
                statement.setString(2, employee.name());
                statement.setString(3, String.format("90000000%03d", employee.resourceId()));
                statement.setString(4, "fixture-" + employee.resourceId() + "@praxis.invalid");
                statement.setString(5, "+55110000" + employee.resourceId());
                statement.setInt(6, currentDepartment.resourceId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedAssignments(Connection connection, FixtureScenario scenario)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into public.funcionario_lotacoes_departamento (
                    funcionario_id, departamento_id, effective_from, effective_to
                ) values (?, ?, ?, ?)
                """)) {
            for (AssignmentFixture assignment : scenario.assignments()) {
                statement.setInt(1, scenario.employees().get(assignment.employeeId()).resourceId());
                statement.setInt(2, scenario.departments().get(assignment.departmentId()).resourceId());
                statement.setDate(3, Date.valueOf(assignment.validFrom()));
                if (assignment.validTo() == null) {
                    statement.setNull(4, java.sql.Types.DATE);
                } else {
                    statement.setDate(4, Date.valueOf(assignment.validTo()));
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedAbsences(Connection connection, FixtureScenario scenario)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into public.ferias_afastamentos (
                    id, tipo, data_inicio, data_fim, observacoes, funcionario_id
                ) values (?, ?, ?, ?, ?, ?)
                """)) {
            int resourceId = 1;
            for (AbsenceFixture absence : scenario.absences()) {
                statement.setInt(1, resourceId++);
                statement.setString(2, absence.restrictedType());
                statement.setDate(3, Date.valueOf(absence.start()));
                statement.setDate(4, Date.valueOf(absence.end()));
                statement.setString(5, absence.restrictedNotes());
                statement.setInt(6, scenario.employees().get(absence.employeeId()).resourceId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static DepartmentFixture currentDepartment(
            FixtureScenario scenario,
            String employeeId
    ) {
        return scenario.assignments().stream()
                .filter(assignment -> assignment.employeeId().equals(employeeId))
                .filter(assignment -> !scenario.nowDate().isBefore(assignment.validFrom()))
                .filter(assignment -> assignment.validTo() == null
                        || scenario.nowDate().isBefore(assignment.validTo()))
                .max(Comparator.comparing(AssignmentFixture::validFrom))
                .map(assignment -> scenario.departments().get(assignment.departmentId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scenario employee has no current department: " + employeeId));
    }

    private static void verifyProjection(Connection connection, FixtureScenario scenario)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     select count(*) as analytics_rows,
                            count(distinct funcionario_id) as employee_rows
                     from public.vw_analytics_afastamentos
                     """)) {
            if (!result.next()
                    || result.getInt("analytics_rows") < 1
                    || result.getInt("employee_rows") < 1) {
                throw new IllegalStateException("Golden fixture produced no analytics projection rows.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from public.vw_perfil_heroi where funcionario_id = ?")) {
            for (EmployeeFixture employee : scenario.employees().values()) {
                statement.setInt(1, employee.resourceId());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) != 1) {
                        throw new IllegalStateException(
                                "Missing employee 360 projection for " + employee.semanticId());
                    }
                }
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String baseOperationalSchema() {
        return """
                create table public.cargos (
                    id integer primary key,
                    nome text not null,
                    nivel text not null,
                    descricao text,
                    salario_minimo numeric,
                    salario_maximo numeric
                );
                create table public.departamentos (
                    id integer primary key,
                    nome text not null,
                    codigo varchar(20) not null unique,
                    responsavel_id integer
                );
                create table public.funcionarios (
                    id integer primary key,
                    version bigint not null default 0,
                    nome_completo text not null,
                    cpf varchar(11) not null unique,
                    data_nascimento date not null,
                    email text not null unique,
                    telefone varchar(30) not null,
                    salario numeric not null,
                    data_admissao date not null,
                    ativo boolean not null,
                    cargo_id integer not null references public.cargos(id),
                    departamento_id integer not null references public.departamentos(id),
                    foto_perfil_url text,
                    estado_civil text,
                    pais_nascimento text,
                    cidade_nascimento text
                );
                alter table public.departamentos
                    add constraint fk_departamento_responsavel
                    foreign key (responsavel_id) references public.funcionarios(id);
                create table public.ferias_afastamentos (
                    id integer primary key,
                    tipo text not null,
                    data_inicio date not null,
                    data_fim date not null,
                    observacoes text,
                    funcionario_id integer not null references public.funcionarios(id)
                );
                """;
    }

    private static String heroProfileView() {
        return """
                create view public.vw_perfil_heroi as
                select
                    f.id as funcionario_id,
                    f.foto_perfil_url as avatar_url,
                    f.nome_completo,
                    null::text as codinome,
                    null::text as universo,
                    false as exposicao_publica,
                    c.nome as cargo,
                    d.nome as departamento,
                    null::integer as score_publico,
                    null::integer as score_governamental,
                    null::numeric as score_medio,
                    '-'::text as habilidades,
                    null::text as equipe_principal,
                    null::text as base_principal
                from public.funcionarios f
                join public.cargos c on c.id = f.cargo_id
                join public.departamentos d on d.id = f.departamento_id;
                """;
    }

    private static void ensureUniqueResourceIds(
            Map<String, EmployeeFixture> employees,
            Map<String, DepartmentFixture> departments
    ) {
        Map<Integer, String> owners = new LinkedHashMap<>();
        employees.values().forEach(employee -> putResourceOwner(
                owners, employee.resourceId(), employee.semanticId()));
        departments.values().forEach(department -> putResourceOwner(
                owners, department.resourceId(), department.semanticId()));
    }

    private static void putResourceOwner(Map<Integer, String> owners, int resourceId, String semanticId) {
        String known = owners.putIfAbsent(resourceId, semanticId);
        if (known != null && !known.equals(semanticId)) {
            throw new IllegalArgumentException(
                    "Resource id " + resourceId + " is shared by " + known + " and " + semanticId);
        }
    }

    private static void requirePositiveIdentity(String semanticId, int resourceId) {
        if (semanticId == null || semanticId.isBlank() || resourceId < 1) {
            throw new IllegalArgumentException("Invalid semantic/resource identity projection.");
        }
    }

    private static String validateDatabaseName(String databaseName) {
        if (databaseName == null || !SAFE_DATABASE_NAME.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("Unsafe disposable database name: " + databaseName);
        }
        return databaseName;
    }

    private static String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    record DatabaseCredentials(String url, String username, String password) {
        static DatabaseCredentials fromEnvironment(Map<String, String> environment) {
            return new DatabaseCredentials(
                    required(environment, "CONFIG_DATASOURCE_URL"),
                    required(environment, "CONFIG_DATASOURCE_USERNAME"),
                    required(environment, "CONFIG_DATASOURCE_PASSWORD"));
        }

        private static String required(Map<String, String> environment, String key) {
            String value = environment.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required environment variable: " + key);
            }
            return value;
        }
    }

    record EmployeeFixture(String semanticId, int resourceId, String name) {
        EmployeeFixture {
            Objects.requireNonNull(semanticId);
            Objects.requireNonNull(name);
        }
    }

    record DepartmentFixture(String semanticId, int resourceId, String name) {
        DepartmentFixture {
            Objects.requireNonNull(semanticId);
            Objects.requireNonNull(name);
        }
    }

    record AssignmentFixture(
            String employeeId,
            String departmentId,
            LocalDate validFrom,
            LocalDate validTo
    ) {
    }

    record AbsenceFixture(
            String semanticId,
            String employeeId,
            LocalDate start,
            LocalDate end,
            String restrictedType,
            String restrictedNotes
    ) {
    }

    record FixtureScenario(
            String id,
            LocalDate nowDate,
            Map<String, EmployeeFixture> employees,
            Map<String, DepartmentFixture> departments,
            List<AssignmentFixture> assignments,
            List<AbsenceFixture> absences
    ) {
    }
}
