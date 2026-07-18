package com.example.praxis.apiquickstart.hr;

import org.postgresql.Driver;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Compiles and invokes the same standalone SQL runner used by operators. */
final class JdbcSqlRunnerProcess {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static Path compiledClasses;

    private JdbcSqlRunnerProcess() {
    }

    static Execution run(String jdbcUrl, String username, String password, Path... sqlFiles) throws Exception {
        return run(jdbcUrl, username, password,
                "jdbc:postgresql://invalid-config-host.invalid/config",
                "must-not-be-used", "must-not-be-used", sqlFiles);
    }

    static Execution run(String jdbcUrl, String username, String password,
                         String configJdbcUrl, String configUsername, String configPassword,
                         Path... sqlFiles) throws Exception {
        Path output = Files.createTempFile("jdbc-sql-runner-", ".log");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command(sqlFiles));
            processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(output.toFile());
            processBuilder.environment().put("SPRING_DATASOURCE_URL", jdbcUrl);
            processBuilder.environment().put("SPRING_DATASOURCE_USERNAME", username);
            processBuilder.environment().put("SPRING_DATASOURCE_PASSWORD", password);
            processBuilder.environment().put("CONFIG_DATASOURCE_URL", configJdbcUrl);
            processBuilder.environment().put("CONFIG_DATASOURCE_USERNAME", configUsername);
            processBuilder.environment().put("CONFIG_DATASOURCE_PASSWORD", configPassword);

            Process process = processBuilder.start();
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("JdbcSqlRunner process exceeded " + TIMEOUT);
            }
            return new Execution(process.exitValue(), Files.readString(output, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static List<String> command(Path[] sqlFiles) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", executable("java")).toString());
        command.add("-cp");
        command.add(compiledClasses() + File.pathSeparator + postgresDriverLocation());
        command.add("JdbcSqlRunner");
        for (Path sqlFile : sqlFiles) {
            command.add(sqlFile.toAbsolutePath().normalize().toString());
        }
        return command;
    }

    private static synchronized Path compiledClasses() throws Exception {
        if (compiledClasses != null) {
            return compiledClasses;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A full JDK is required to compile scripts/JdbcSqlRunner.java");
        }

        compiledClasses = Files.createTempDirectory("jdbc-sql-runner-classes-");
        int result = compiler.run(null, null, null,
                "--release", "21",
                "-encoding", "UTF-8",
                "-d", compiledClasses.toString(),
                Path.of("scripts/JdbcSqlRunner.java").toString());
        if (result != 0) {
            throw new IllegalStateException("Failed to compile scripts/JdbcSqlRunner.java; javac exit=" + result);
        }
        return compiledClasses;
    }

    private static Path postgresDriverLocation() throws Exception {
        return Path.of(Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? name + ".exe" : name;
    }

    record Execution(int exitCode, String output) {
    }
}
